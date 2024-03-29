package eu.darkcode.lifestealaddon.playerdata;

public interface ThrowableFunction <T, R> {
    R apply(T t) throws Throwable;
}
