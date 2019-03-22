package danila.org.ticketscanner.util.function;

@FunctionalInterface
public interface Function<T> {
    void invoke(T arg);
}
