package kldemo.contactsearching;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactIntentService extends IntentService {
    final static String CONTACT_FOUND = "contact_found";
    final static String CONTACT_LIST_UPDATED = "contact_list_updated";
    final static String QUERY = "query";
    final static String PREV_QUERY = "prev_query";
    final static String CONTACT_LIST = "contact_list";
    final static String QUERY_RESULT_CONTACT = "query_result_contact";
    final static String QUERY_RESULT_INDEX = "query_result_index";
    private boolean isDestroyed = false;
    private String DISPLAY_NAME = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? ContactsContract.Contacts.DISPLAY_NAME_PRIMARY : ContactsContract.Contacts.DISPLAY_NAME;
    private HashMap<String, String[]> correspondHashMap = new HashMap<String, String[]>() {
        {
            put("2", new String[]{"А", "Б", "В", "Г", "A", "B", "C"});
            put("3", new String[]{"Д", "Е", "Ж", "З", "D", "E", "F"});
            put("4", new String[]{"И", "Й", "К", "Л", "G", "H", "I"});
            put("5", new String[]{"М", "Н", "О", "П", "J", "K", "L"});
            put("6", new String[]{"Р", "С", "Т", "У", "M", "N", "O"});
            put("7", new String[]{"Ф", "Х", "Ц", "Ч", "P", "Q", "R", "S"});
            put("8", new String[]{"Ш", "Щ", "Ъ", "Ы", "T", "U", "V"});
            put("9", new String[]{"Ь", "Э", "Ю", "Я", "W", "X", "Y", "Z"});
        }
    };
    private String prevQuery = "";
    private ArrayList<Contact> contactList = new ArrayList<>();


    public ContactIntentService() {
        super("ContactIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String query = intent.getStringExtra(QUERY);
        prevQuery = intent.getStringExtra(PREV_QUERY);
        contactList = intent.getParcelableArrayListExtra(CONTACT_LIST);
        getContacts(query);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isDestroyed = false;
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        isDestroyed = true;
        super.onDestroy();
    }

    private void getContacts(String query) {
        if (query.isEmpty()) {
            getAllContacts();
        } else {
            getContactsByQuery(query);
        }
    }

    private void getContactsByQuery(String query) {
        if (prevQuery.isEmpty()) {
            setContactList(query);
        }
        StringBuilder queryBuilder = new StringBuilder(query);
        List<String> symbolCombinationList = null;
        HashMap<String, String[]> tempCorrespondHashMap = new HashMap<String, String[]>();
        HashMap<Integer, List<String>> symbolCombinationHashMap = new HashMap<>();
        String currentSymbol = "";
        String[] correspondSymbols = null;
        int query_length = query.length();
        List<Integer> combCountList = new ArrayList<>();
        int index = 0;
        int combCount = 0;
        for (int i_query = 0; i_query < query_length; i_query++) {
            currentSymbol = String.valueOf(queryBuilder.charAt(i_query));
            symbolCombinationList = new ArrayList<>();
            List<String> uniqueSymbolList = new ArrayList<>();

            for (Contact contact : contactList) {
                StringBuilder nameStringBuilder = new StringBuilder(contact.name);
                String nameCurrentSymbol = "";
                for (int i_name = 0, length = nameStringBuilder.length(); i_name < length; i_name++) {
                    nameCurrentSymbol = String.valueOf(nameStringBuilder.charAt(i_name)).toUpperCase();
                    if (!uniqueSymbolList.contains(nameCurrentSymbol)) {
                        uniqueSymbolList.add(nameCurrentSymbol);
                    }
                }
            }

            for (Map.Entry<String, String[]> entry : correspondHashMap.entrySet()) {
                String key = entry.getKey();
                String[] symbols = entry.getValue();
                List<String> tempSymbolList = new ArrayList<>();
                for (String nameCurrentSymbol : symbols) {
                    if (uniqueSymbolList.contains(nameCurrentSymbol)) {
                        tempSymbolList.add(nameCurrentSymbol);
                    }
                }
                if (tempSymbolList.size() > 0) {
                    tempCorrespondHashMap.put(key, tempSymbolList.toArray(new String[0]));
                }
            }

            symbolCombinationList.add(currentSymbol);
            correspondSymbols = tempCorrespondHashMap.get(currentSymbol);
            if (correspondSymbols != null) {
                for (String symbol : correspondSymbols) {
                    symbolCombinationList.add(symbol);
                }
            }
            symbolCombinationHashMap.put(i_query, symbolCombinationList);

            List<String> stepSymbolCombinationList = symbolCombinationHashMap.get(i_query);
            int stepSymbolCombinationListSize = stepSymbolCombinationList.size();
            if (i_query == 0) {
                combCount = stepSymbolCombinationListSize;
            } else {
                combCount = combCount * stepSymbolCombinationListSize;
            }
            combCountList.add(combCount);
        }

        for (Contact contact : contactList) {
            int result = searchCombination(contact, query, index, combCount, combCountList, symbolCombinationHashMap);
            if (result == 1) {
                index++;
            } else if (result == -1) {
                return;
            }
        }

        if (index == 0 && !isDestroyed) {
            sendContact(null, index);
        }
    }

    private int searchCombination(Contact contact, String query, int index, int combCount, List<Integer> combCountList, HashMap<Integer, List<String>> symbolCombinationHashMap) {
        for (int i_comb = 0; i_comb < combCount; i_comb++) {
            if (!isDestroyed) {
                String queryCombination = getQueryCombination(query, i_comb, combCount, combCountList, symbolCombinationHashMap);
                if (contact.name.toUpperCase().indexOf(queryCombination) >= 0
                        || (contact.number != null && contact.number.toUpperCase().indexOf(queryCombination) >= 0)) {
                    contact.query = queryCombination;
                    sendContact(contact, index);
                    return 1;
                }
            } else {
                return -1;
            }
        }
        return 0;
    }

    private String getQueryCombination(String query, int i_comb, int combCount, List<Integer> combCountList, HashMap<Integer, List<String>> symbolCombinationHashMap) {
        StringBuilder stringBuilder = new StringBuilder(query);
        for (int i_query = 0, query_length = stringBuilder.length(); i_query < query_length; i_query++) {
            List<String> symbolCombinationList = symbolCombinationHashMap.get(i_query);
            int symbolIndex = i_comb;
            if (i_query == query_length - 1) {
                symbolIndex = i_comb % symbolCombinationList.size();
            } else {
                int prevCombCount = 0;
                int nextCombCount = combCount / combCountList.get(i_query);
                if (i_query > 0) {
                    prevCombCount = combCount / combCountList.get(i_query - 1);
                }
                if (prevCombCount > 0 && i_comb >= prevCombCount) {
                    symbolIndex = symbolIndex - prevCombCount * (i_comb / prevCombCount);
                }
                symbolIndex = symbolIndex / nextCombCount;
            }
            stringBuilder.replace(i_query, i_query + 1, symbolCombinationList.get(symbolIndex));
        }
        return stringBuilder.toString().toUpperCase();
    }

    private void setContactList(String query) {
        contactList = new ArrayList<>();
        String selection = "";
        List<String> selectionArgs = new ArrayList<String>();
        List<Contact> contacts = getContactsByNumber(query);
        if (contacts.size() > 0) {
            contactList.addAll(contacts);
            List<String> contactsId = new ArrayList<>();
            for (Contact contact : contacts) {
                contactsId.add(contact.id);
            }
            selection = ContactsContract.Contacts._ID + " NOT IN (" + TextUtils.join(",", contactsId) + ")";
            selection += " AND";
        }
        String currentSymbol = String.valueOf(query.charAt(0));
        selection += " (" + DISPLAY_NAME + " LIKE ?";
        selectionArgs.add("%" + currentSymbol + "%");
        String[] correspondSymbols = correspondHashMap.get(currentSymbol);
        if (correspondSymbols != null) {
            for (String symbol : correspondSymbols) {
                selection += " OR " + DISPLAY_NAME + " LIKE ?";
                selectionArgs.add("%" + symbol + "%");
            }
        }
        selection += ")";
        contacts = getContactsByName(selection, selectionArgs.toArray(new String[0]));
        contactList.addAll(contacts);
        Collections.sort(contactList);
        sendContactList();
    }

    private List<Contact> getContactsByNumber(String query) {
        ContentResolver contentResolver = getContentResolver();
        List<Contact> contacts = new ArrayList<>();
        Contact contact = null;
        Cursor pnoneCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ?",
                new String[]{"%" + query + "%"},
                null);
        while (pnoneCursor.moveToNext()) {
            contact = new Contact();
            contact.id = pnoneCursor.getString(pnoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
            contact.number = pnoneCursor.getString(pnoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            Cursor contactsCursor = contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    new String[]{
                            ContactsContract.Contacts._ID,
                            DISPLAY_NAME,
                            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                            ContactsContract.Contacts.HAS_PHONE_NUMBER},
                    ContactsContract.Contacts._ID + " = ?",
                    new String[]{contact.id},
                    null);
            while (contactsCursor.moveToNext()) {
                contact.name = contactsCursor.getString(contactsCursor.getColumnIndex(DISPLAY_NAME));
                contact.imageUri = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI));
            }
            contact.query = query;
            contacts.add(contact);
        }
        return contacts;
    }

    private List<Contact> getContactsByName(String selection, String[] selectionArgs) {
        ContentResolver contentResolver = getContentResolver();
        List<Contact> contacts = new ArrayList<>();
        Contact contact = null;
        int hasPhone = 0;
        Cursor contactsCursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{
                        ContactsContract.Contacts._ID,
                        DISPLAY_NAME,
                        ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                        ContactsContract.Contacts.HAS_PHONE_NUMBER},
                selection,
                selectionArgs,
                DISPLAY_NAME + " ASC");

        while (contactsCursor.moveToNext()) {
            contact = new Contact();
            contact.id = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts._ID));
            contact.name = contactsCursor.getString(contactsCursor.getColumnIndex(DISPLAY_NAME));
            contact.imageUri = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI));
            hasPhone = contactsCursor.getInt(contactsCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
            if (hasPhone > 0) {
                Cursor pnoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{contact.id},
                        null);
                while (pnoneCursor.moveToNext()) {
                    contact.number = pnoneCursor.getString(pnoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                }
            }
            contacts.add(contact);
        }
        return contacts;
    }

    private void getAllContacts() {
        ContentResolver contentResolver = getContentResolver();
        Cursor contactsCursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{
                        ContactsContract.Contacts._ID,
                        DISPLAY_NAME,
                        ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                        ContactsContract.Contacts.HAS_PHONE_NUMBER},
                null,
                null,
                DISPLAY_NAME + " ASC");

        int index = 0;
        Contact contact = null;
        int hasPhone = 0;
        while (contactsCursor.moveToNext()) {
            if (!isDestroyed) {
                contact = new Contact();
                contact.id = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts._ID));
                contact.name = contactsCursor.getString(contactsCursor.getColumnIndex(DISPLAY_NAME));
                contact.imageUri = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI));
                hasPhone = contactsCursor.getInt(contactsCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                if (hasPhone > 0) {
                    Cursor pnoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{contact.id},
                            DISPLAY_NAME + " ASC");
                    while (pnoneCursor.moveToNext()) {
                        contact.number = pnoneCursor.getString(pnoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    }
                }
                sendContact(contact, index);
                index++;
            } else {
                return;
            }
        }
    }

    private void sendContact(Contact contact, int index) {
        Intent intent = new Intent();
        intent.setAction(CONTACT_FOUND);
        intent.putExtra(QUERY_RESULT_CONTACT, contact);
        intent.putExtra(QUERY_RESULT_INDEX, index);
        sendBroadcast(intent);
    }

    private void sendContactList() {
        Intent intent = new Intent();
        intent.setAction(CONTACT_LIST_UPDATED);
        intent.putParcelableArrayListExtra(CONTACT_LIST, contactList);
        sendBroadcast(intent);
    }
}
