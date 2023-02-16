package ru.ratnikov.ShowBillBot.model;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

/**
 * @author Michael Gosling
 */
@Data
@Entity(name = "person")
public class Person {

    private static final String ID = "chat_id";
    private static final String FIRST_NAME = "first_name";
    private static final String LAST_NAME = "last_name";
    private static final String USER_NAME = "user_name";
    private static final String REGISTERED = "registered_at";
    private static final String JOIN_TABLE = "person_event";
    private static final String JOIN_COLUMN = "chat_id";
    private static final String INV_JOIN_COLUMN = "event_id";

    @Id
    @Column(name = ID)
    private long chatId;

    @Column(name = FIRST_NAME, length = 100)
    private String firstName;

    @Column(name = LAST_NAME, length = 100)
    private String lastName;

    @Column(name = USER_NAME, length = 100)
    private String userName;

    @Column(name = REGISTERED)
    private Timestamp registeredAt;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = JOIN_TABLE,
            joinColumns = @JoinColumn(name = JOIN_COLUMN),
            inverseJoinColumns = @JoinColumn(name = INV_JOIN_COLUMN)
    )
    private List<Event> events;

    @Transient
    private boolean isRegistered;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person)) return false;
        Person person = (Person) o;
        return this.chatId == person.getChatId() || this.userName.equals(person.getUserName());
    }

    @Override
    public int hashCode() {
        var userNameHash = userName.hashCode();
        return (int)(31 * chatId) + (31 * userNameHash);
    }

    public boolean isRegistered() {
        return !this.userName.isEmpty();
    }
}
