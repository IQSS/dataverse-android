package org.dataverse.android.dataverse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {

    private final String SEARCH_API_RESPONSE = "searchApiResponse";
    private final String SEARCH_RESULTS = "searchResults";
    private final String START_VALUE = "startValue";
    Button previousButton;
    private EditText searchQueryEditText;
    /**
     * TODO Rename "result" to "message" or something. It's no longer the search result.
     */
    private String result = "";
    private List<SearchResult> searchResults;
    private int start;
    private int rows = 10;
    private Integer totalCount;
    private String searchServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupSearchServer();
        setContentView(R.layout.activity_main);
        previousButton = (Button) findViewById(R.id.previousButton);

        searchQueryEditText = (EditText) findViewById(R.id.searchQueryEditText);
        searchQueryEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    TextView searchResultsTextView = (TextView) findViewById(R.id.searchResultsTextView);
                    onSearchClick(searchResultsTextView);
                    return true;
                }
                return false;
            }
        });

        if (savedInstanceState != null) {
            // http://developer.android.com/training/basics/activity-lifecycle/recreating.html
            result = savedInstanceState.getString(SEARCH_API_RESPONSE);
            TextView searchResultsTextView = (TextView) findViewById(R.id.searchResultsTextView);
            searchResultsTextView.setText(result);
            searchResults = savedInstanceState.getParcelableArrayList(SEARCH_RESULTS);
            start = savedInstanceState.getInt(START_VALUE);
        } else {
            searchResults = new ArrayList<>();
            start = 0;
        }
        ListAdapter listAdapter = new SearchResultsAdapter(this, searchResults);
        ListView searchResultsListView = (ListView) findViewById(R.id.searchResultsListView);
        searchResultsListView.setAdapter(listAdapter);
        searchResultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                String clicked = "You selected " + String.valueOf(parent.getItemAtPosition(position));
//                Toast.makeText(MainActivity.this, clicked, Toast.LENGTH_SHORT).show();
                SearchResult searchResult = (SearchResult) parent.getItemAtPosition(position);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(searchResult.getUrl()));
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(START_VALUE, start);
        outState.putString(SEARCH_API_RESPONSE, result);
        outState.putParcelableArrayList(SEARCH_RESULTS, (ArrayList<? extends android.os.Parcelable>) searchResults);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onSearchClick(View view) {

        searchQueryEditText = (EditText) findViewById(R.id.searchQueryEditText);

        if (validQueryEntered(searchQueryEditText)) {
            Toast.makeText(this, getString(R.string.search_query_input_valid) + " " + searchServer, Toast.LENGTH_SHORT).show();
            hideKeyboard(searchQueryEditText);
            start = 0;
            new GetSearchResults().execute();

        } else {
            Toast.makeText(this, getString(R.string.search_query_input_invalid), Toast.LENGTH_LONG).show();
        }

    }

    private void hideKeyboard(EditText searchQueryEditText) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchQueryEditText.getWindowToken(), 0);
    }

    protected boolean validQueryEntered(EditText editText) {
        if (editText.getText().toString().trim().length() == 0) {
            return false;
        } else {
            return true;
        }
    }

    public void onPreviousClick(View view) {
        start -= rows;
        Toast.makeText(MainActivity.this, "Getting results from " + start, Toast.LENGTH_SHORT).show();
        new GetSearchResults().execute();
    }

    public void onNextClick(View view) {
        start += rows;
        Toast.makeText(MainActivity.this, "Getting results from " + start, Toast.LENGTH_SHORT).show();
        new GetSearchResults().execute();
    }

    private void setupSearchServer() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        searchServer = sharedPref.getString("pref_server_list", null);
        if (searchServer != null) {
            if (searchServer.equals("custom")) {
                searchServer = sharedPref.getString("custom_server", null);
            }
        } else {
            // sane default
            searchServer = "apitest.dataverse.org";
        }
    }

    class GetSearchResults extends AsyncTask<Void, Void, Void> {

        String jsonString = "";

        @Override
        protected Void doInBackground(Void... params) {
            EditText searchQueryEditText = (EditText) findViewById(R.id.searchQueryEditText);
            String query = searchQueryEditText.getText().toString().replace(" ", "+");
            String startParam = "";
            if (start > 0) {
                startParam = "&start=" + start;
            }

            DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());
            URI url = null;
            try {
                url = URI.create("http://" + searchServer + "/api/search?q=" + query + startParam);
            } catch (IllegalArgumentException e) {
                // newline in search query
                result = getString(R.string.search_query_unparseable);
                return null;
            }
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Content-type", "application/json");
            InputStream inputStream = null;
            try {
                HttpResponse response = httpClient.execute(httpGet);
                HttpEntity entity = response.getEntity();
                inputStream = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                jsonString = sb.toString();
//                System.out.println("jsonString: " + jsonString);
                JSONObject jsonObject = new JSONObject(jsonString);
                outputSearchData(jsonObject);
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                // unable to resolve host in DNS, for example
                result = e.getLocalizedMessage();
                searchResults = new ArrayList<>();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            TextView searchResultsTextView = (TextView) findViewById(R.id.searchResultsTextView);
            searchResultsTextView.setText(result);
            ListAdapter listAdapter = new SearchResultsAdapter(getApplicationContext(), searchResults);
            ListView searchResultsListView = (ListView) findViewById(R.id.searchResultsListView);
            searchResultsListView.setAdapter(listAdapter);
        }

        private void outputSearchData(JSONObject jsonObject) {
            JSONObject data = null;
            try {
                data = jsonObject.getJSONObject("data");
            } catch (JSONException e) {
                e.printStackTrace();
                result = getString(R.string.search_query_unparseable);
                searchResults = new ArrayList<>();
                return;
            }

            searchResults = new ArrayList<>();
            try {
                totalCount = data.getInt("total_count");
                if (totalCount == 0) {
                    result = getString(R.string.search_query_no_results);
                } else if (totalCount == 1) {
                    result = totalCount.toString() + " " + getString(R.string.search_results_singular);
                } else {
                    result = totalCount.toString() + " " + getString(R.string.search_results_plural);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (start == 0) {
                            previousButton.setEnabled(false);
                        } else {
                            previousButton.setEnabled(true);
                        }
                        if (start > totalCount) {
                            ((Button) findViewById(R.id.nextButton)).setEnabled(false);
                        } else if (start + rows > totalCount) {
                            ((Button) findViewById(R.id.nextButton)).setEnabled(false);
                        } else {
                            ((Button) findViewById(R.id.nextButton)).setEnabled(true);
                        }
                    }
                });

                JSONArray items = data.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = (JSONObject) items.get(i);
                    String display = "";
                    String name = item.getString("name");
                    String type = item.getString("type");
                    if (type.equals("dataverse")) {
                        display = name + " Dataverse";
                    } else if (type.equals("dataset")) {
                        String citation = item.getString("citation");
                        String globalId = item.getString("global_id");
                        JSONArray authorsArray = item.getJSONArray("authors");
                        String authors = "";
                        for (int j = 0; j < authorsArray.length(); j++) {
                            authors += authorsArray.getString(j) + ", ";
                        }
                        display = name + ", " + authors + globalId;
                    } else if (type.equals("file")) {
                        String fileType = item.getString("file_type");
                        display = name + " (" + fileType + ")";
                    }
                    String url = item.getString("url");
                    String imageUrl = item.getString("image_url");
                    searchResults.add(new SearchResult(display, url, imageUrl));
                }

            } catch (JSONException e) {
                e.printStackTrace();
                result = e.getLocalizedMessage();
                searchResults = new ArrayList<>();
            }
        }

    }


}
