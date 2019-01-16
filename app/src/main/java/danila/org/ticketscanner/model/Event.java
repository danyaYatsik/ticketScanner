package danila.org.ticketscanner.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Event implements Parcelable {

    public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>() {
        // распаковываем объект из Parcel
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        public Event[] newArray(int size) {
            return new Event[size];
        }
    };
    private String name;
    private String description;
    private String id;

    public Event(String name, String description, String id) {
        this.name = name;
        this.description = description;
        this.id = id;
    }

    // конструктор, считывающий данные из Parcel
    private Event(Parcel parcel) {
        name = parcel.readString();
        description = parcel.readString();
        id = parcel.readString();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(name);
        parcel.writeString(description);
        parcel.writeString(id);
    }
}
