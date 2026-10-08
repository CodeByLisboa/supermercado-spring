package supermercado.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import supermercado.excecao.DadosInvalidosException;
import supermercado.excecao.PedidoNaoEncontradoException;
import supermercado.excecao.ProdutoNaoEncontradoException;
import supermercado.excecao.RegraNegocioException;
import supermercado.web.dto.ErroDTO;

/**
 * Converte as exceções em respostas JSON com o código HTTP certo.
 *
 * <ul>
 *   <li>400 - dados digitados com erro ({@link DadosInvalidosException}) ou requisição mal formada;</li>
 *   <li>404 - pedido ou produto que não existe;</li>
 *   <li>422 - operação que a regra de negócio não permite ({@link RegraNegocioException});</li>
 *   <li>500 - erro inesperado (detalhe só no log do servidor).</li>
 * </ul>
 */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger LOG = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(DadosInvalidosException.class)
    public ResponseEntity<ErroDTO> dadosInvalidos(DadosInvalidosException erro) {
        return resposta(HttpStatus.BAD_REQUEST, erro.getMessage(), erro);
    }

    @ExceptionHandler({PedidoNaoEncontradoException.class, ProdutoNaoEncontradoException.class})
    public ResponseEntity<ErroDTO> naoEncontrado(RegraNegocioException erro) {
        return resposta(HttpStatus.NOT_FOUND, erro.getMessage(), null);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroDTO> regraDeNegocio(RegraNegocioException erro) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, erro.getMessage(), null);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroDTO> requisicaoInvalida(Exception erro) {
        LOG.debug("Requisição inválida", erro);
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida. Confira os dados enviados.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroDTO> erroInesperado(Exception erro) {
        // Erros do próprio Spring MVC (rota inexistente, método errado...) já sabem o código HTTP
        if (erro instanceof ErrorResponse respostaDoSpring) {
            HttpStatus status = HttpStatus.valueOf(respostaDoSpring.getStatusCode().value());
            return resposta(status, status == HttpStatus.NOT_FOUND ? "Endereço não encontrado." : status.getReasonPhrase(), null);
        }
        LOG.error("Erro inesperado na API", erro);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado no servidor. Tente de novo.", null);
    }

    private static ResponseEntity<ErroDTO> resposta(HttpStatus status, String mensagem, DadosInvalidosException campos) {
        ErroDTO corpo = new ErroDTO(status.value(), status.getReasonPhrase(), mensagem,
                campos == null ? null : campos.getErros());
        return ResponseEntity.status(status).body(corpo);
    }
}
