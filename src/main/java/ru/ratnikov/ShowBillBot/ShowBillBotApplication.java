package ru.ratnikov.ShowBillBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.ratnikov.ShowBillBot.service.ShowbillBot;

@SpringBootApplication
public class ShowBillBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShowBillBotApplication.class, args);
	}

}
