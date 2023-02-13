package ru.ratnikov.ShowBillBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ratnikov.ShowBillBot.model.Event;

/**
 * @author Michael Gosling
 */
@Repository
public interface EventsRepository extends JpaRepository <Event, Long> {

}
