package ru.ratnikov.ShowBillBot.model;

import lombok.Data;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author Michael Gosling
 */
@Data
@Entity(name = "event")
public class Event {
    @Id
    @Column(name = "event_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long eventId;

    @Column(name = "title", length = 200)
    String title;

    @Column(name = "description", length = 500)
    String description;

    @Column(name = "seats")
    int seats;

    @Column(name = "date_of_event")
    @Temporal(TemporalType.TIMESTAMP)
    Date date;

    @ManyToMany(mappedBy = "events",
            fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    private List<Person> people;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Event event = (Event) o;

        return this.eventId == event.getEventId() || this.title.equals(event.getTitle());
    }

    @Override
    public int hashCode() {
        var titleHash = title.hashCode();
        var descHash = description.hashCode();
        return (int)(31 * eventId) + (31 * seats) + (31 * titleHash) + (31 * descHash);
    }
}
