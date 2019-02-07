package danila.org.ticketscanner.util;

@FunctionalInterface
public interface Function<T> {
    void invoke(T arg);
}
