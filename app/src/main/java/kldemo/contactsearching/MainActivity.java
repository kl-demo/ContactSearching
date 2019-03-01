package kldemo.contactsearching;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final static int PERMISSIONS_REQUEST_READ_CONTACTS = 1;
    private String prevQuery = "";
    private ArrayList<Contact> contactList = new ArrayList<>();
    private RecyclerView wordRecyclerView;
    private ContactAdapter adapter;
    private TextView textViewQuery;
    private ContactReciver contactReciver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wordRecyclerView = findViewById(R.id.contacts_recycler_view);
        wordRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        registerContactReceiver();

        textViewQuery = findViewById(R.id.text_view_query);

        onCnangeQuery();

        View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                String tag = v.getTag().toString();
                if (tag.equals("0")) {
                    StringBuilder stringBuilder = new StringBuilder(textViewQuery.getText());
                    stringBuilder.insert(0, "+");
                    textViewQuery.setText(stringBuilder);
                    onCnangeQuery();
                    return true;
                }
                return false;
            }
        };

        Button btn0 = findViewById(R.id.btn_0);
        btn0.setOnLongClickListener(onLongClickListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(contactReciver);
    }

    public void onPhoneButtonClick(View v) {
        String tag = v.getTag().toString();
        StringBuilder stringBuilder = new StringBuilder(textViewQuery.getText());
        if (tag.equals("backspace")) {
            int length = stringBuilder.length();
            if (length > 0) {
                stringBuilder.deleteCharAt(length - 1);
            }
        } else {
            stringBuilder.append(tag);
        }
        textViewQuery.setText(stringBuilder);
        onCnangeQuery();
    }

    private void onCnangeQuery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            onGrantPermissionsResult();
        }
    }

    private void onGrantPermissionsResult() {
        sendQuery(textViewQuery.getText().toString());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onGrantPermissionsResult();
                }
                return;
            }
        }
    }

    private void sendQuery(String query) {
        Intent intent = new Intent(getApplicationContext(), ContactIntentService.class);
        intent.putExtra(ContactIntentService.QUERY, query);
        intent.putExtra(ContactIntentService.PREV_QUERY, prevQuery);
        intent.putParcelableArrayListExtra(ContactIntentService.CONTACT_LIST, contactList);
        stopService(intent);
        startService(intent);
        prevQuery = query;
    }

    private void updateContactList(Contact contact, boolean clearPrevData) {
        List<Contact> contacts = new ArrayList<>();
        if (adapter == null) {
            if (contact != null) {
                contacts.add(contact);
            }
            adapter = new ContactAdapter(contacts);
            wordRecyclerView.setAdapter(adapter);
        } else {
            if (clearPrevData) {
                adapter.clearContacts();
            }
            if (contact != null) {
                adapter.addContact(contact);
            }
            adapter.notifyDataSetChanged();
        }
    }

    private void registerContactReceiver() {
        contactReciver = new ContactReciver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ContactIntentService.CONTACT_FOUND);
        intentFilter.addAction(ContactIntentService.CONTACT_LIST_UPDATED);
        registerReceiver(contactReciver, intentFilter);
    }

    private class ContactReciver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ContactIntentService.CONTACT_FOUND:
                    Contact contact = intent.getParcelableExtra(ContactIntentService.QUERY_RESULT_CONTACT);
                    int index = intent.getIntExtra(ContactIntentService.QUERY_RESULT_INDEX, 0);
                    updateContactList(contact, index == 0);
                    break;
                case ContactIntentService.CONTACT_LIST_UPDATED:
                    contactList = intent.getParcelableArrayListExtra(ContactIntentService.CONTACT_LIST);
                    break;
            }
        }
    }

    private class ContactHolder extends RecyclerView.ViewHolder {
        private Contact contact;
        private ImageView contactImageView;
        private TextView contactNameTextView;
        private TextView contactNumberTextView;
        private String highlightColor = "#3F51B5";

        public ContactHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.contact_item, parent, false));

            contactImageView = itemView.findViewById(R.id.contact_image);
            contactNameTextView = itemView.findViewById(R.id.contact_name);
            contactNumberTextView = itemView.findViewById(R.id.contact_number);
        }

        public void bind(Contact contact) {
            this.contact = contact;
            contactImageView.setImageBitmap(uriToBitmap(contact.imageUri));
            contactNameTextView.setText(highlightText(contact.name, contact.query));
            contactNumberTextView.setText(highlightText(contact.number, contact.query));
        }

        private SpannableString highlightText(String text, String query) {
            if (text != null && query != null) {
                SpannableString spannableString = new SpannableString(text);
                int index = text.toUpperCase().indexOf(query);
                if (index >= 0) {
                    spannableString.setSpan(new BackgroundColorSpan(Color.YELLOW), index, index + query.length(), 0);
                }
                return spannableString;
            }
            return new SpannableString(text != null ? text : "");
        }

        private Bitmap uriToBitmap(String uri) {
            Bitmap bitmap = null;
            if (uri != null) {
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(uri));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.person);
            }
            return bitmap;
        }
    }

    private class ContactAdapter extends RecyclerView.Adapter<ContactHolder> {
        private List<Contact> contacts;

        public ContactAdapter(List<Contact> contacts) {
            this.contacts = contacts;
        }

        @Override
        public ContactHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            return new ContactHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(ContactHolder holder, int position) {
            Contact word = contacts.get(position);
            holder.bind(word);
        }

        @Override
        public int getItemCount() {
            return contacts.size();
        }

        public void clearContacts() {
            this.contacts.clear();
        }

        public void addContact(Contact contacts) {
            this.contacts.add(contacts);
        }
    }
}
