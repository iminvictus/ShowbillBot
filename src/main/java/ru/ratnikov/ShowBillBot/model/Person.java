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

    @Id
    @Column(name = "chat_id")
    private long chatId;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "registered_at")
    private Timestamp registeredAt;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "person_event",
            joinColumns = @JoinColumn(name = "chat_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id")
    )
    private List<Event> events;

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
}
