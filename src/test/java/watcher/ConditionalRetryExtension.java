package watcher;

import org.junit.jupiter.api.extension.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;

/**
 * Extensão de retry condicional: só re-executa o teste se a falha for
 * causada por uma exceção "transiente" (timeout, conexão recusada, etc.).
 * Falhas de assertion (AssertionError) NUNCA são re-executadas, porque
 * essas indicam um bug real, não instabilidade de ambiente.
 *
 * Uso:
 *   @Test
 *   @RetryOnTransientFailure(maxAttempts = 3)
 *   void meuTeste() { ... }
 */
public class ConditionalRetryExtension implements TestExecutionExceptionHandler {

    // Exceções consideradas "transientes" (justificam retry).
    // Comparação feita por NOME da classe (e não instanceof) para incluir também
    // exceções de bibliotecas HTTP internas do Rest-Assured/Apache HttpClient
    // sem precisar declarar uma dependência direta a essas classes aqui.
    private static final List<Class<? extends Throwable>> TRANSIENT_EXCEPTIONS = Arrays.asList(
            SocketTimeoutException.class,
            ConnectException.class,
            java.net.UnknownHostException.class
    );

    // Nomes (simples) de exceções transientes adicionais, verificadas por herança/nome
    // sem import direto — cobre casos como org.apache.http.conn.ConnectTimeoutException
    private static final List<String> TRANSIENT_EXCEPTION_NAME_FRAGMENTS = Arrays.asList(
            "ConnectTimeoutException",
            "NoHttpResponseException",
            "SocketException"
    );

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        Method testMethod = context.getRequiredTestMethod();
        RetryOnTransientFailure annotation = testMethod.getAnnotation(RetryOnTransientFailure.class);

        // Sem a anotação, ou falha não é transiente: propaga o erro normalmente (sem retry)
        if (annotation == null || !isTransient(throwable)) {
            throw throwable;
        }

        int maxAttempts = annotation.maxAttempts();
        ExtensionContext.Store store = context.getStore(ExtensionContext.Namespace.create(ConditionalRetryExtension.class, testMethod));
        Integer currentAttempt = store.getOrDefault("attempt", Integer.class, 1);

        if (currentAttempt >= maxAttempts) {
            System.err.printf(
                    "[ConditionalRetry] %s esgotou %d tentativas. Última falha: %s%n",
                    testMethod.getName(), maxAttempts, throwable.getClass().getSimpleName()
            );
            throw throwable;
        }

        System.out.printf(
                "[ConditionalRetry] %s falhou com %s (tentativa %d/%d) — re-executando...%n",
                testMethod.getName(), throwable.getClass().getSimpleName(), currentAttempt, maxAttempts
        );

        store.put("attempt", currentAttempt + 1);

        // Re-executa o método de teste diretamente. Como a invocação é via
        // reflexão, qualquer exceção lançada dentro do teste chega aqui
        // embrulhada em InvocationTargetException — precisamos desembrulhar
        // e reprocessar pela MESMA lógica de decisão (para continuar contando
        // tentativas e permitir múltiplos retries, não só um).
        try {
            testMethod.invoke(context.getRequiredTestInstance());
        } catch (java.lang.reflect.InvocationTargetException reflectionWrapper) {
            Throwable actualCause = reflectionWrapper.getCause();
            handleTestExecutionException(context, actualCause);
        }
    }

    private boolean isTransient(Throwable throwable) {
        // Nunca considera AssertionError como transiente, mesmo que esteja na lista por engano
        if (throwable instanceof AssertionError) {
            return false;
        }
        boolean matchesKnownType = TRANSIENT_EXCEPTIONS.stream()
                .anyMatch(exceptionClass -> exceptionClass.isInstance(throwable));
        if (matchesKnownType) {
            return true;
        }

        String className = throwable.getClass().getSimpleName();
        return TRANSIENT_EXCEPTION_NAME_FRAGMENTS.stream().anyMatch(className::contains);
    }

    /**
     * Anotação para marcar testes elegíveis a retry condicional.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @ExtendWith(ConditionalRetryExtension.class)
    public @interface RetryOnTransientFailure {
        int maxAttempts() default 3;
    }
}