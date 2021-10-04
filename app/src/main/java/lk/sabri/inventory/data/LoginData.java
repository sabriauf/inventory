package lk.sabri.inventory.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class LoginData {

    @PrimaryKey
    private int id;
    private String name;
    private int level;
    private String username;
    private String password;
    private String last_invoice_id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLast_invoice_id() {
        return last_invoice_id;
    }

    public void setLast_invoice_id(String last_invoice_id) {
        this.last_invoice_id = last_invoice_id;
    }
}
