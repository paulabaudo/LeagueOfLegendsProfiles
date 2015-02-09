package com.globant.paulabaudo.leagueoflegensprofiles;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class LolProfilesFragment extends Fragment {

    EditText mEditTextSummoner;
    TextView mTextViewChampions;
    final static String LOG_TAG = LolProfilesFragment.class.getSimpleName();

    public LolProfilesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_lol_profiles, container, false);
        mEditTextSummoner = (EditText) rootView.findViewById(R.id.edit_text_summoner);
        mTextViewChampions = (TextView) rootView.findViewById(R.id.text_view_champions);
        Button buttonGetMatches = (Button) rootView.findViewById(R.id.button_get_matches);
        buttonGetMatches.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String summoner = mEditTextSummoner.getText().toString();
                String message = String.format(getString(R.string.getting_champions_for_summoner), summoner);
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                new FetchReposTask().execute(summoner);
            }
        });
        return rootView;
    }

    class FetchReposTask extends AsyncTask<String, Void, String> { //CAMBIAR

        final static String API_KEY = "?api_key=e1452383-1e5a-4842-a15d-f89568f612af";
        final static String RIOT_BASE_URL = "las.api.pvp.net";
        final static String API_PATH = "api";
        final static String LOL_PATH = "lol";
        final static String LAS_PATH = "las";
        final static String VERSION_MATCHHISTORY = "v2.2";
        final static String VERSION_SUMMONER = "v1.4";
        final static String VERSION_CHAMPION = "v1.2";
        final static String BY_NAME_PATH = "by-name";
        final static String INFO_PATH_SUMMONER = "summoner";
        final static String INFO_PATH_MATCHHISTORY = "matchhistory";
        final static String INFO_PATH_CHAMPION = "champion";
        final static String STATICDATA_PATH = "static-data";


        @Override
        protected String doInBackground(String... params) {
            String summoner;
            String listOfChampions = "";
            int idSummoner = 0;
            if (params.length>0){
                summoner = params[0];
            } else {
                summoner = "Apuli"; //My summoner
            }
            try {
                URL urlSummonerId = constructURLSummonerIdQuery(summoner);
                HttpURLConnection httpURLSummonerIdConnection = (HttpURLConnection) urlSummonerId.openConnection();
                try {
                    String responseIdSummoner = readFullResponse(httpURLSummonerIdConnection.getInputStream());
                    idSummoner = parseResponseIdSummoner(responseIdSummoner);
                    List<Integer> championsHistory = getChampionsHistoryFromMatches(idSummoner);
                    listOfChampions = getChampions(championsHistory);
                } catch (IOException e){
                    e.printStackTrace();
                } finally {
                    httpURLSummonerIdConnection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return listOfChampions;
        }

        private String getChampions(List<Integer> championsHistory) {
            List <String> champions = new ArrayList<>();
            for (int championId : championsHistory){
                champions.add(getChampionName(championId));
            }
            return TextUtils.join(", ", champions);
        }

        private String getChampionName(int championId) {
            String name = "";
            try {
                URL urlChampion = constructURLChampionQuery(championId);
                HttpURLConnection httpURLChampionConnection = (HttpURLConnection) urlChampion.openConnection();
                try {
                    String responseChampion = readFullResponse(httpURLChampionConnection.getInputStream());
                    name = parseResponseChampion(responseChampion);
                } catch (IOException e){
                    e.printStackTrace();
                } finally {
                    httpURLChampionConnection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return name;
        }

        private String parseResponseChampion(String responseChampion) {
            final String CHAMPION_NAME = "name";
            String name = "";
            try {
                JSONArray responseJsonArray = new JSONArray(responseChampion);
                JSONObject object;
                for (int i = 0; i < responseJsonArray.length(); i++){
                    object = responseJsonArray.getJSONObject(i);
                    name = object.getString(CHAMPION_NAME);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return name;
        }

        private URL constructURLChampionQuery(int championId) throws MalformedURLException {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https").authority(RIOT_BASE_URL).
                    appendPath(API_PATH).
                    appendPath(LOL_PATH).
                    appendPath(STATICDATA_PATH).
                    appendPath(LAS_PATH).
                    appendPath(VERSION_CHAMPION).
                    appendPath(INFO_PATH_CHAMPION).
                    appendPath(Integer.toString(championId)+API_KEY).
                    appendPath(API_KEY);
            Uri uri = builder.build();
            Log.d(LOG_TAG, "Built URI ChampionName: " + uri.toString());
            return new URL(uri.toString());
        }

        private List<Integer> getChampionsHistoryFromMatches(int idSummoner) {
            List<Integer> championsHistory = new ArrayList<>();
            try {
                URL urlMatchHistory = constructURLMatchHistoryQuery(idSummoner);
                HttpURLConnection httpURLMatchHistoryConnection = (HttpURLConnection) urlMatchHistory.openConnection();
                try {
                    String responseMatchHistory = readFullResponse(httpURLMatchHistoryConnection.getInputStream());
                    championsHistory = parseResponseMatchHistory(responseMatchHistory);
                } catch (IOException e){
                    e.printStackTrace();
                } finally {
                    httpURLMatchHistoryConnection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return championsHistory;
        }

        private List<Integer> parseResponseMatchHistory(String responseMatchHistory) {
            final String PARTICIPANTS = "participants";
            final String CHAMPIONID = "championId";
            List<Integer> champions = new ArrayList<>();
            try {
                JSONArray responseJsonArray = new JSONArray(responseMatchHistory);
                JSONObject object;
                for (int i = 0; i < responseJsonArray.length(); i++){
                    object = responseJsonArray.getJSONObject(i);
                    champions.add(object.getJSONObject(PARTICIPANTS).getInt(CHAMPIONID));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return champions;
        }

        private URL constructURLMatchHistoryQuery(int idSummoner) throws MalformedURLException {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https").authority(RIOT_BASE_URL).
                    appendPath(API_PATH).
                    appendPath(LOL_PATH).
                    appendPath(LAS_PATH).
                    appendPath(VERSION_MATCHHISTORY).
                    appendPath(INFO_PATH_MATCHHISTORY).
                    appendPath(Integer.toString(idSummoner)+API_KEY).
                    appendPath(API_KEY);
            Uri uri = builder.build();
            Log.d(LOG_TAG, "Built URI MatchHistory: " + uri.toString());
            return new URL(uri.toString());
        }

        private int parseResponseIdSummoner(String responseIdSummoner) {
            final String SUMMONER_ID = "id";
            int id = 0;
            try {
                JSONArray responseJsonArray = new JSONArray(responseIdSummoner);
                JSONObject object;
                for (int i = 0; i < responseJsonArray.length(); i++){
                    object = responseJsonArray.getJSONObject(i);
                    id = object.getInt(SUMMONER_ID);
                    Log.d(LOG_TAG, "Summoner id: "+id);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d(LOG_TAG, "Summoner not found");
            }
            return id;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            mTextViewChampions.setText(response);
        }

        private String readFullResponse(InputStream inputStream) throws IOException {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String response = null;
            String line;
            while ((line= bufferedReader.readLine()) != null){
                stringBuilder.append(line).append("\n");
            }
            if (stringBuilder.length()>0){
                response = stringBuilder.toString();
            }
            return response;
        }

        private URL constructURLSummonerIdQuery(String summoner) throws MalformedURLException { //CAMBIAR
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https").authority(RIOT_BASE_URL).
                    appendPath(API_PATH).
                    appendPath(LOL_PATH).
                    appendPath(LAS_PATH).
                    appendPath(VERSION_SUMMONER).
                    appendPath(INFO_PATH_SUMMONER).
                    appendPath(BY_NAME_PATH).
                    appendPath(summoner+API_KEY);
            Uri uri = builder.build();
            Log.d(LOG_TAG, "Built URI SummonerId: " + uri.toString());
            return new URL(uri.toString());
        }
    }
}