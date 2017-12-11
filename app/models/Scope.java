package models;

//import org.mongodb.morphia.annotations.Entity;
//import org.mongodb.morphia.annotations.Id;

/**
 * Scope record for clients
 * Created by Selim Eren Bekçe on 15.08.2017.
 */
//@Entity
public class Scope {
//    @Id
    private String id;
    /**
     * belonging client
     */
    private String clientId;
    /**
     * e.g. 'email'
     */
    private String scope;
    /**
     * e.g. 'Read your email address'
     */
    private String description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
