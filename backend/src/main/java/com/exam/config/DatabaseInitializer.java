package com.exam.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing database...");
        ClassPathResource resource = new ClassPathResource("schema.sql");
        String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        
        // 解析 SQL 语句，正确处理多行语句
        List<String> statements = parseSqlStatements(sql);
        
        for (String statement : statements) {
            try {
                jdbcTemplate.execute(statement);
                log.debug("Executed: {}", statement.substring(0, Math.min(50, statement.length())) + "...");
            } catch (Exception e) {
                log.warn("Statement failed: {} - Error: {}", 
                    statement.substring(0, Math.min(50, statement.length())), e.getMessage());
            }
        }
        log.info("Database initialization completed");
    }
    
    private List<String> parseSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (String line : sql.split("\n")) {
            String trimmedLine = line.trim();
            
            // 跳过空行和注释
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) {
                continue;
            }
            
            current.append(line).append("\n");
            
            // 如果行以分号结尾，则为完整语句
            if (trimmedLine.endsWith(";")) {
                String stmt = current.toString().trim();
                // 移除末尾分号
                if (stmt.endsWith(";")) {
                    stmt = stmt.substring(0, stmt.length() - 1).trim();
                }
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current = new StringBuilder();
            }
        }
        
        return statements;
    }
}
