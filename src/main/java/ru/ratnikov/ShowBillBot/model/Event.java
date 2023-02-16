package ru.ratnikov.ShowBillBot.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * @author Michael Gosling
 */
@Data
@Entity(name = "event")
public class Event {
    private static final String ID = "event_id";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String SEATS = "seats";
    private static final String DATE = "date_of_event";
    private static final String MAPPED_BY = "events";

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long eventId;

    @Column(name = TITLE, length = 200)
    String title;

    @Column(name = DESCRIPTION, length = 500)
    String description;

    @Column(name = SEATS)
    int seats;

    @Column(name = DATE)
    @Temporal(TemporalType.TIMESTAMP)
    Date date;

    @ManyToMany(mappedBy = MAPPED_BY,
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
