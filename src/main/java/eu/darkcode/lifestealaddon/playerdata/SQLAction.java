package eu.darkcode.lifestealaddon.playerdata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;

public interface SQLAction <R> {
    @Nullable R execute(PreparedStatement statement) throws Throwable;
    void prepare(@NotNull PreparedStatement statement) throws Throwable;
    @NotNull String sql();
}
