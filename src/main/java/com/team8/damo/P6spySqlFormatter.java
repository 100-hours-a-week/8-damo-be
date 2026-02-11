package com.team8.damo;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import jakarta.annotation.PostConstruct;
import org.hibernate.engine.jdbc.internal.FormatStyle;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class P6spySqlFormatter implements MessageFormattingStrategy {
    private static final String RESET = "\u001B[0m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String PURPLE = "\u001B[35m";
    private static final String YELLOW = "\u001B[33m";

    @PostConstruct
    public void setLogMessageFormat() {
        P6SpyOptions.getActiveInstance().setLogMessageFormat(this.getClass().getName());
    }

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        sql = formatSql(category, sql);
        Date currentDate = new Date();

        SimpleDateFormat format1 = new SimpleDateFormat("yy.MM.dd HH:mm:ss");
        String highlightedSql = highlightKeywords(sql);

        //return now + "|" + elapsed + "ms|" + category + "|connection " + connectionId + "|" + P6Util.singleLine(prepared) + sql;
        return format1.format(currentDate) + " | "+ "OperationTime : "+ elapsed + "ms" + highlightedSql;
    }

    private String formatSql(String category,String sql) {
        if(sql == null || sql.trim().isEmpty()) return sql;

        // Only format Statement, distinguish DDL And DML
        if (Category.STATEMENT.getName().equals(category) || Category.BATCH.getName().equals(category)) {
            String tmpsql = sql.trim().toLowerCase(Locale.ROOT);
            if(tmpsql.startsWith("create") || tmpsql.startsWith("alter") || tmpsql.startsWith("comment")) {
                sql = FormatStyle.DDL.getFormatter().format(sql);
            }else {
                sql = FormatStyle.BASIC.getFormatter().format(sql);
            }
            sql = "|\nHeFormatSql(P6Spy sql,Hibernate format):"+ sql;
        }

        return sql;
    }

    private String highlightKeywords(String sql) {
        if (sql == null) return null;

        return sql
            // 주요 키워드 파란색
            .replaceAll("(?i)\\b(select|from|where|join|inner join|left join|right join|on)\\b",
                BLUE + "$1" + RESET)
            // DML 키워드 노란색
            .replaceAll("(?i)\\b(insert into|update|delete|values|set|insert|into)\\b",
                YELLOW + "$1" + RESET)
            // 그룹/정렬 키워드 보라색
            .replaceAll("(?i)\\b(group by|order by|limit|having)\\b",
                PURPLE + "$1" + RESET)
            // 논리 연산자 청록색
            .replaceAll("(?i)\\b(and|or|not|in|exists|is null|is not null|like|between)\\b",
                CYAN + "$1" + RESET);
    }
}
