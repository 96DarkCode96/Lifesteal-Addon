package eu.darkcode.lifestealaddon.playerdata;

public interface ThrowableConsumer<T> {
    void accept(T t) throws Throwable;
}
