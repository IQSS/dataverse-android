package org.dataverse.android.dataverse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

        EditText searchQueryEditText = (EditText) findViewById(R.id.searchQueryEditText);

        if (validQueryEntered(searchQueryEditText)) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            String server = sharedPref.getString("server", null);
            if (server == null || server.isEmpty()) {
                // FIXME DRY! Hard-coded in preferences.xml too.
                server = "dataverse-demo.iq.harvard.edu";
            }
            Toast.makeText(this, getString(R.string.search_query_input_valid) + " " + server, Toast.LENGTH_SHORT).show();
            hideKeyboard(searchQueryEditText);
            new GetSearchResults(server).execute();

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

    class GetSearchResults extends AsyncTask<Void, Void, Void> {

        GetSearchResults(String server) {
            this.server = server;
        }

        String jsonString = "";
        String result = "";
        String server = "";

        @Override
        protected Void doInBackground(Void... params) {
            EditText searchQueryEditText = (EditText) findViewById(R.id.searchQueryEditText);
            String query = searchQueryEditText.getText().toString().replace(" ", "+");
            DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());
            URI url = null;
            try {
                url = URI.create("http://" + server + "/api/search?q=" + query);
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
                System.out.println("jsonString: " + jsonString);
                JSONObject jsonObject = new JSONObject(jsonString);
                outputSearchData(jsonObject);
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                // unable to resolve host in DNS, for example
                result = e.getLocalizedMessage();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            TextView searchResultsTextView = (TextView) findViewById(R.id.searchResultsTextView);
            searchResultsTextView.setText(result);
        }

        private void outputSearchData(JSONObject jsonObject) {
            JSONObject data = null;
            try {
                data = jsonObject.getJSONObject("data");
            } catch (JSONException e) {
                e.printStackTrace();
                result = getString(R.string.search_query_unparseable);
                return;
            }

            try {
                String items = data.getString("items");
                String[] results = items.split(",");
                for (int i = 0; i < results.length; i++) {
                    result = result + results[i] + "\n";
                }
            } catch (JSONException e) {
                e.printStackTrace();
                result = e.getLocalizedMessage();
            }
        }

    }


}
