import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TaggingGUI extends DrawingGUI {
    private static final int width = 1000, height = 650;        // setup: size of the "world" (window)
    private int delay = 100;                                    // a delay for the timer to fire (and repeat) in alliseconds

    private boolean homeScreen = true;                          // do we want to draw the home screen
    private boolean taggingScreen = false;                      // do we want to draw the tagging screen
    private boolean predictingScreen = false;                   // do we want to draw the predicting screen

    private Map<String, String> tagMeaningsMap = new HashMap<>();   // keeps track of all of the tags and their meanings

    private boolean search = false;                             // should we draw the search bar
    private String searchField = "";                            // what are we typing in the search bar
    private String maxSearchField = "";                                 // if the search field is full, keep track of what fits

    private String tags = "";                                   // what tags did we find from this search
    private boolean typing = false;                             // are we typing
    private boolean clickedSearch = false;                       // do we start tagging the current search field word

    private ArrayList<Map<String, String>> predictions;         // what are the best predictions based on the incomplete sentence

    public TaggingGUI() {

        super("Tagging Game", width, height); //set up graphics "world"

        // create the tags to meaning map
        tagMeanings("PS5/tagMeaning.txt");

        // Timer drives the animations
        setTimerDelay(delay);
        startTimer();
    }

    /**
     * Create a map where the key is the tag and the value is the meaning of the tag
     * @param filename      // a file we created to store tag meanings
     */
    public void tagMeanings(String filename) {
        // The text where we want to extract the information from
        BufferedReader tagsFile;

        // Open the file, if possible
        try {
            tagsFile = new BufferedReader(new FileReader(filename));
        }

        catch (FileNotFoundException e) {
            System.err.println("Cannot open file.\n" + e.getMessage());
            return;
        }

        // Read the file if possible
        try {
            String tagMeaning;

            while ((tagMeaning = tagsFile.readLine()) != null) {
                // separate each meaning and tag onto a String Array
                String[] tags = tagMeaning.split("/");

                // if the input file is incorrect
                if (tags.length != 2) {
                    System.out.println("Please fix tagsMeaning file");
                    return;
                }

                // inserts the tag and its meaning to the map
                tagMeaningsMap.put(tags[0], tags[1]);
            }
        }
        catch (IOException e) {
            System.err.println("IO error while reading.\n" + e.getMessage());
        }

        // Close the files, if possible
        try {
            tagsFile.close();
            tagsFile.close();
        }
        catch (IOException e) {
            System.err.println("Cannot close file.\n" + e.getMessage());
        }
    }

    /**
     * DrawingGUI method, draws the game interface
     */
    @Override
    public void draw(Graphics g) {
        // set up the scene

        // if we are on the home screen
        if (homeScreen) {
            // draw the background
            g.setColor(new Color(184, 184, 184));
            g.fillRect(0, 0, width, height);

            // draw the game title
            g.setColor(new Color(13, 76, 108));
            g.fillRect(20, 20, width - 40, 150);
            String gameTitle = "Parts of Speech Game";
            Font boldTitle = new Font("Futura", Font.BOLD, 70);
            g.setColor(new Color(255, 255, 255));
            g.setFont(boldTitle);
            g.drawString(gameTitle, (width - g.getFontMetrics().stringWidth(gameTitle)) / 2, ((150 + g.getFontMetrics().getHeight()) / 2) - 10);

            // Write instructions
            Font simpleText = new Font("Helvetica Nue", Font.BOLD, 25);
            g.setFont(simpleText);
            g.setColor(new Color(0, 0, 0));
            String instructions = "Choose the activity you would like to participate in:";
            g.drawString(instructions, (width - g.getFontMetrics().stringWidth(instructions)) / 2, 210);

            // draw options buttons
            Font options = new Font("Helvetica Nue", Font.BOLD, 40);
            g.setFont(options);
            int buttonWidth = 250;
            int buttonHeight = 100;
            // tagging button
            g.setColor(new Color(154, 215, 234));
            g.fillRect((width/2) - buttonWidth, 240, buttonWidth * 2, buttonHeight);
            // predictions button
            g.setColor(new Color(	73, 183, 217));
            g.fillRect((width/2) - buttonWidth, 360, buttonWidth * 2, buttonHeight);
            // quit button
            g.setColor(new Color(		26, 103, 127));
            g.fillRect((width/2) - buttonWidth, 490, buttonWidth * 2, buttonHeight);
            // write the strings
            g.setColor(new Color(0, 0, 0));
            g.drawString("Tagging", (width - g.getFontMetrics().stringWidth("tagging")) / 2, 300);
            g.drawString("Predicting", (width - g.getFontMetrics().stringWidth("Predicting")) / 2, 420);
            g.drawString("Quit Game", (width - g.getFontMetrics().stringWidth("Quit Game")) / 2, 550);
        }

        // if we pressed the tagging button
        if (taggingScreen || predictingScreen) {
            // draw the background
            g.setColor(new Color(255, 255, 255));
            g.fillRect(0, 0, width, height);

            // draw the game title
            g.setColor(new Color(13, 76, 108));
            g.fillRect(20, 20, width - 40, 150);
            String gameTitle = "Parts of Speech Game";
            Font boldTitle = new Font("Futura", Font.BOLD, 70);
            g.setColor(new Color(255, 255, 255));
            g.setFont(boldTitle);
            g.drawString(gameTitle, (width - g.getFontMetrics().stringWidth(gameTitle)) / 2, ((150 + g.getFontMetrics().getHeight()) / 2) - 10);

            // draw the search bar
            search = true;

            // draw the exit and go back buttons
            Font simpleText = new Font("Helvetica Nue", Font.BOLD, 25);
            g.setFont(simpleText);

            g.setColor(new Color(13, 76, 108));
            // exit button
            g.fillRect(20, height - 60,  70, 40);
            // go back button
            g.fillRect(100, height - 60,  120, 40);
            // the strings
            g.setColor(new Color(255, 255, 255));
            g.drawString("Exit", (30), height - 30);
            g.drawString("Go Back", (110), height - 30);
        }

        // if we are on the tagging screen or on the predicting screen, draw the search bar
        if (search) {
            // create the search bar
            g.setColor(new Color(184, 184, 184));
            g.fillRect(40, 180, width - 80, 80);

            g.setColor(new Color(0, 0, 0));
            Font simpleText = new Font("Helvetica Nue", Font.BOLD, 25);
            g.setFont(simpleText);
            g.setColor(new Color(0, 0, 0));
            g.drawString("Sentence:", (50), 230);

            g.setColor(new Color(255, 255, 255));
            g.fillRect(200, 190, 550, 60);

            // search button
            g.setColor(new Color(125, 125, 125));
            g.fillRect(780, 195, 150, 50);
            g.setColor(new Color(0, 0, 0));
            g.drawString("Search", (810), 230);
        }

        // if we are typing, draw the string we are typing
        if (search && typing) {
            Font simpleText = new Font("Helvetica Nue", Font.BOLD, 25);
            g.setFont(simpleText);
            g.setColor(new Color(0, 0, 0));

            // stop writing once search field reaches the maximum length
            if (g.getFontMetrics().stringWidth(searchField) < 540) {
                g.drawString(searchField, 210, 230);
                maxSearchField = searchField;
            }
            // just draw the biggest max search field we can fiend
            else {
                g.drawString(maxSearchField, 210, 230);
            }
        }

        // if we are on the tagging screen and we clicked the search button
        if (taggingScreen && clickedSearch) {
            Font simpleText = new Font("Helvetica Nue", Font.BOLD, 25);
            g.setFont(simpleText);
            g.setColor(new Color(0, 0, 0));

            // stop writing once search field reaches the maximum length
            if (g.getFontMetrics().stringWidth(searchField) < 540) {
                g.drawString(searchField, 210, 230);
                maxSearchField = searchField;
            }
            // just draw the longest sentence we can fit
            else {
                g.drawString(maxSearchField, 210, 230);
            }

            // write the first line of strings at this y (increment for the next onw)
            int y = 300;

            String[] tagResults = tags.split(" ");
            String[] words = searchField.split(" ");

            // of the amount of tags does not equal the amount of words
            if (tagResults.length != words.length) {
                g.drawString("Please insert a another sentence", width - 20 - g.getFontMetrics().stringWidth("Please insert a another sentence"), height - 20);
            }

            else {
                for (int i = 0; i < tagResults.length; i++) {

                    // if the text is too large to calculate the tags in the screen
                    // unless it's the last word (then print that one)
                    if (y > height - 70 && i != tagResults.length - 1) {
                        g.drawString("Please insert a smaller sentence", width - 20 - g.getFontMetrics().stringWidth("Please insert a smaller sentence"), height - 20);
                        break;
                    }

                    // draw the meaning of the tag
                    else {
                        // if it's a valid tag we have saved (ignores punctuation)
                        if (tagMeaningsMap.containsKey(tagResults[i])) {

                            // check if the location is correct
                            if (450 - g.getFontMetrics().stringWidth(words[i]) > 0) {
                                // draw the word we are tagging
                                g.drawString(words[i], 450 - g.getFontMetrics().stringWidth(words[i]), y);
                                // draw the tag next to it
                                g.drawString(tagMeaningsMap.get(tagResults[i]), 500, y);
                            }
                            // if the word's location is smaller than 0, ask for a smaller word
                            else {
                                g.drawString("Please insert a smaller sentence", width - 20 - g.getFontMetrics().stringWidth("Please insert a smaller sentence"), height - 20);

                            }
                        }
                    }
                    // increase it's y location
                    y += 40;
                }
            }
        }

        // if we are on the predicting screen and we clicked the search button
        if (predictingScreen && clickedSearch) {

            Font simpleText = new Font("Helvetica Nue", Font.BOLD, 25);
            g.setFont(simpleText);
            g.setColor(new Color(0, 0, 0));

            // stop writing once search field reaches the maximum length
            if (g.getFontMetrics().stringWidth(searchField) < 540) {
                g.drawString(searchField, 210, 230);
                maxSearchField = searchField;
            }
            // draw the largest sentence that fits
            else {
                g.drawString(maxSearchField, 210, 230);
            }

            // start writing the first prediction at this height, increase height for the next one
            int y = 300;

            // can only fit 9 lines on the screen, keep track of how many we have printed
            int maxNumbPrinted = 0;

            // for every slot on the array list
            for (int i = 0; i < predictions.size(); i++) {

                // can only fit 9 lines on the screen
                if (maxNumbPrinted < 9) {
                    // if the text is too large to calculate the tags in the screen
                    // unless it's the last word (then print that one)

                    // draw the best word predictions for this sentence
                    // get the word found in this slot of the array
                    String predictedWord = "";
                    for (String word : predictions.get(i).keySet()) {
                        predictedWord = word;
                        break;
                    }

                    // get the tag of this word
                    String tag = predictions.get(i).get(predictedWord);

                    // if it's a valid tag we have saved (ignores punctuation)
                    if (tagMeaningsMap.containsKey(tag)) {

                        // check if the location is correct
                        if (450 - g.getFontMetrics().stringWidth(predictedWord) > 0) {
                            g.drawString(predictedWord, 450 - g.getFontMetrics().stringWidth(predictedWord), y);
                            g.drawString(tagMeaningsMap.get(tag), 500, y);

                            // increase it's y location and marked that we printer a word
                            maxNumbPrinted += 1;
                            y += 40;
                        }
                        // if the word's location is smaller than 0, ask for a smaller word
                        else {
                            g.drawString("Please insert a smaller sentence", width - 20 - g.getFontMetrics().stringWidth("Please insert a smaller sentence"), height - 20);
                        }
                    }
                }
                // stop if we have already reached the max amount of words to be printed
                else {
                    break;
                }
            }
        }
    }

    /**
     * DrawingGUI method, keeps track of where we pressed the mouse
     */
    @Override
    public void handleMousePress(int x, int y) {
        if (homeScreen) {
            // if the tagged option was pressed
            if (x > (width / 2) - 250 && x < (width / 2) + 250) {
                if (y > 240 && y < 340) {
                    taggingScreen = true;
                    homeScreen = false;
                    predictingScreen = false;
                    repaint();
                }

                // if the prediction button was pressed
                if (y > 360 && y < 460) {
                    taggingScreen = false;
                    homeScreen = false;
                    predictingScreen = true;
                    repaint();
                }

                // if the quit button was pressed
                if (y > 490 && y < 590) {
                    System.exit(0);

                }
            }
        }

        // if we are on the search screen and had already typed something, allow the search button to be clicked
        if (search && typing) {
            // location of the search button
            if (x > 780 && x < 780 + 150) {
                if (y > 195 && y < 245) {
                    // start tagging the sentence
                    clickedSearch = true;
                    // stop typing
                    typing = false;

                    // if we clicked search, find the tags and predictions of the current search field word
                    Sudi tagging = new Sudi();
                    tagging.trainMachine("PS5/brown-train-sentences.txt", "PS5/brown-train-tags.txt");
                    tags = tagging.ViterbiDecoding(searchField);
                    predictions = tagging.predictNext(searchField);
                    repaint();
                }
            }
        }

        // if we pressed on the search bar, we can start typing
        if (search) {
            // the location of the white part of the search bar
            if (x > 200 && x < 750) {
                if (y > 190 && y < 270) {
                    // clear the last search field
                    searchField = "";
                    // stop tagging
                    clickedSearch = false;
                    // mark that we are typing
                    typing = true;
                    // reset the image
                    repaint();
                }
            }
        }

        // configure go back and exit buttons mouse press
        if (taggingScreen || predictingScreen) {

            // since they are both the same height
            if (y > height-60 && y < height-20 ) {

                // if we pressed the exit button
                if (x > 20 && x < 90) {
                    // exit the game
                    System.exit(0);
                }
                // if we press back button
                if (x > 100 && x < 220) {
                    // clear everything and go back to the home screen
                    homeScreen = true;
                    taggingScreen = false;
                    predictingScreen = false;
                    search = false;
                    typing = false;
                    clickedSearch = false;
                    searchField = "";
                    repaint();
                }
            }
        }
    }

    /**
     * DrawingGUI method, keeps track of what we are typing
     */
    @Override
    public void handleKeyPress(char k) {
        // if we are in the tagging or predicting screen (meaning we need a search bar) and clicked the search bar to type
        if (search && typing) {

            // if we haven't pressed the search button yet
            if (!clickedSearch) {
                // allow the user to type
                searchField += Character.toString(k);

                // if we press backspace, remove parts of the string
                if (k == ((char) 8)&&searchField.length()>1) {

                    searchField = searchField.substring(0, searchField.length() - 2);
                }
                // repaint the string
                repaint();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new TaggingGUI();
            }
        }); }
}