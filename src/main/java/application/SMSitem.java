package application;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SMSitem {
	private final StringProperty dateProp;
	private final StringProperty paramProp;
	private final StringProperty fullProp;
	private final StringProperty addressProp;
	private final StringProperty contactProp;
	
    /**
     * Конструктор по умолчанию.
     */
    public SMSitem() {
        this("хер знает когда", "", "хер знает что", "хер знает от кого", "хер знает от кого");
    }
    
    public SMSitem(String date, String param, String full, String address, String contact) {
    	this.dateProp = new SimpleStringProperty(date);
    	this.paramProp = new SimpleStringProperty(param);
    	this.fullProp = new SimpleStringProperty(full);
    	this.addressProp = new SimpleStringProperty(address);
    	this.contactProp = new SimpleStringProperty(contact);
    }

    public StringProperty dateProperty() {
    	return dateProp;
    }
    public StringProperty paramProperty() {
    	return paramProp;
    }
    public StringProperty fullProperty() {
    	return fullProp;
    }
    public StringProperty addressProperty() {
    	return addressProp;
    }
    public StringProperty contactProperty() {
    	return contactProp;
    }
    

}
