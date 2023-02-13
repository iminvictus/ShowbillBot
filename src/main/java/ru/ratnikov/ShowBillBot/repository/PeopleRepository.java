package ru.ratnikov.ShowBillBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ratnikov.ShowBillBot.model.Person;

import java.util.List;

/**
 * @author Michael Gosling
 */
@Repository
public interface PeopleRepository extends JpaRepository<Person, Long> {
    List<Person> findByChatId(Long chatId);
}
