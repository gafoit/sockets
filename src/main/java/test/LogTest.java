package test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogTest {
    private static final Logger logger = LoggerFactory.getLogger(LogTest.class);

    public static void main(String[] args) {
        logger.info("Пример логирования: приложение запущено!");
        logger.error("Пример ошибки: тестовая ошибка!");
    }
}
