package eu.darkcode.lifestealaddon.playerdata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;

public final class SQLActionBuilder<O> {

    private String sql;
    private @Nullable ThrowableConsumer<PreparedStatement> prepare = null;
    private final ThrowableFunction<PreparedStatement, O> function;

    public static <R> SQLActionBuilder<R> function(@NotNull ThrowableFunction<PreparedStatement, R> function) {
        return new SQLActionBuilder<>(function);
    }

    private SQLActionBuilder(@NotNull ThrowableFunction<PreparedStatement, O> function) {
        this.function = function;
    }

    public SQLActionBuilder<O> sql(@NotNull String sql) {
        this.sql = sql;
        return this;
    }

    public SQLActionBuilder<O> prepare(@Nullable ThrowableConsumer<PreparedStatement> prepare) {
        this.prepare = prepare;
        return this;
    }

    public SQLAction<O> build() {
        if(sql == null) throw new IllegalStateException("SQL not set");
        return new SQLActionImpl<>(sql, prepare, function);
    }

    public O execute(@NotNull Connection conn) throws Throwable {
        SQLAction<O> act = build();
        PreparedStatement ps = conn.prepareStatement(act.sql());
        act.prepare(ps);
        return act.execute(ps);
    }

    public O retry(@NotNull Connection conn, int retries) throws Throwable {
        SQLAction<O> act = build();
        PreparedStatement ps = conn.prepareStatement(act.sql());
        act.prepare(ps);
        while(retries > 0) {
            try {
                return act.execute(ps);
            } catch (Throwable e) {
                retries--;
            }
        }
        return act.execute(ps);
    }

    private record SQLActionImpl<T>(@NotNull String sql, @Nullable ThrowableConsumer<@NotNull PreparedStatement> prepare, @NotNull ThrowableFunction<@NotNull PreparedStatement, @Nullable T> function) implements SQLAction<T> {
        @Nullable
        @Override
        public T execute(PreparedStatement ps) throws Throwable {
            return function.apply(ps);
        }

        @Override
        public void prepare(@NotNull PreparedStatement ps) throws Throwable {
            if (prepare != null) prepare.accept(ps);
        }
    }

}