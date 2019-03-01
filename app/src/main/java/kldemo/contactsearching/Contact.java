package kldemo.contactsearching;

import android.os.Parcel;
import android.os.Parcelable;

public class Contact implements Comparable<Contact>, Parcelable {
    public String id;

    public String name;

    public String number;

    public String imageUri;

    public String query;

    public Contact() {
    }

    protected Contact(Parcel in) {
        id = in.readString();
        name = in.readString();
        number = in.readString();
        imageUri = in.readString();
        query = in.readString();
    }

    @Override
    public int compareTo(Contact contact) {
        return this.name.compareTo(contact.name);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(number);
        dest.writeString(imageUri);
        dest.writeString(query);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Contact> CREATOR = new Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel in) {
            return new Contact(in);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };
}
