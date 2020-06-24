import java.io.*;
import java.util.*;

/**
 * @author Lizzie Hernandez Videa
 * Partner: María Paula Mora
 * Monday November 4th
 *
 * Program that can be trained to identify patterns in word tagging and then produce the corresponding sequence of tags
 */

public class SudiEC {
    public static boolean debugFlag = false;
    public static boolean simpleTest = true;
    public static boolean brownTest = false;
    public static boolean test1 = false;
    public static boolean shortenedBrown = false;


    // graph with tags as vertices and transitions frequencies between tags as edges
    public Map<String, Map<String, Map<String,Integer>>> pairToNextMap;

    public Map<String, Map<String, Integer>> pairsFrequency;

    // map matching a word with all its possible tags and the amount of times it has been seen with that tag
    public Map<String, Map<String, Integer>> observationsMap;

    public Map<String, Integer> tagFrequency;

    /**
     *  Constructor no parameters
     */
    public SudiEC(){
        pairToNextMap = new HashMap<>();
        pairsFrequency = new HashMap<>();
        observationsMap = new HashMap<>();
        tagFrequency = new HashMap<>();
    }

    public class DoubleString{
        String previous;
        String current;

        public DoubleString(String current, String previous){
            this.previous = previous;
            this.current = current;
        }
    }

    /**
     * Given a file, recognize patterns in tags, that is modify the graph according to training data
     * @param originalFileName File with words
     * @param tagFileName File with parts of speech for corresponding words
     */
    public void trainMachine(String originalFileName, String tagFileName){
        BufferedReader input1;
        BufferedReader input2;

        // Open both files, if possible
        try {
            input1 = new BufferedReader(new FileReader(originalFileName));
            input2 = new BufferedReader(new FileReader(tagFileName));
        }
        catch (FileNotFoundException e) {
            System.err.println("Cannot open file.\n" + e.getMessage());
            return;
        }
        // try reading both files
        try {
            tagFrequency.put("#", 0);

            Map<String, Integer> tempMap = new HashMap<>();
            tempMap.put("#", 0);
            pairsFrequency.put("@", tempMap);
            tagFrequency.put("@", 0);

            pairsFrequency.put("#", new HashMap<>());

            String words;       // words read from original file
            String states;      // tags read from parts of speech file
            while ((words = input1.readLine())!= null&&(states = input2.readLine())!= null) {
                String[] partsOfSpeech = states.split(" ");
                String[] correspondingWords = words.split(" ");

                // for every sentence start with the start of sentence indicator (#)
                String previous = "@";
                String current = "#";
                pairsFrequency.get(previous).put(current, pairsFrequency.get(previous).get(current) + 1);
                tagFrequency.put(previous, tagFrequency.get(previous)+1);
                tagFrequency.put(current, tagFrequency.get(current)+1);

                // loop through words and corresponding parts of speech in a sentence
                int i = 0;
                while (i < partsOfSpeech.length) {
                    // get a word and its corresponding tag
                    String next = partsOfSpeech[i];
                    String word = correspondingWords[i].toLowerCase();

                    if(!tagFrequency.containsKey(next)){
                        tagFrequency.put(next, 0);
                    }
                    tagFrequency.put(next, tagFrequency.get(next)+1);


                    if (!pairsFrequency.containsKey(next)) {
                        pairsFrequency.put(next, new HashMap<>());
                    }


                    if (!pairsFrequency.get(current).containsKey(next)) {
                        pairsFrequency.get(current).put(next, 0);
                    }

                    pairsFrequency.get(current).put(next, pairsFrequency.get(current).get(next) + 1);


                    if (!pairToNextMap.containsKey(next)) {
                        pairToNextMap.put(next, new HashMap<>());
                    }


                    // if there wasn't a connection between current tag and next tag, add a connection with frequency 1
                    if (!pairToNextMap.containsKey(previous)) {
                        pairToNextMap.put(previous, new HashMap<>());
                    }
                    if (!pairToNextMap.get(previous).containsKey(current)) {
                        pairToNextMap.get(previous).put(current, new HashMap<>());
                    }
                    if (!pairToNextMap.get(previous).get(current).containsKey(next)) {
                        pairToNextMap.get(previous).get(current).put(next, 0);
                    }

                    pairToNextMap.get(previous).get(current).put(next, pairToNextMap.get(previous).get(current).get(next) + 1);

                    // if the word hadn't already been seen, add it to observations
                    if (!observationsMap.containsKey(word)) {
                        observationsMap.put(word, new HashMap<>());
                    }
                    // if the word hadn't already been seen with this tag, add the taag to it's map of possible tags
                    if (!observationsMap.get(word).containsKey(next)) {
                        observationsMap.get(word).put(next, 0);
                    }
                    // increment the frequency by which the word has been seen with this tag by 1
                    observationsMap.get(word).put(next, observationsMap.get(word).get(next) + 1);

                    // advance through other words in the sentence
                    previous = current;
                    current = next;
                    i++;
                }
            }
        }
        catch (IOException e) {
            System.err.println("IO error while reading.\n" + e.getMessage());
        }

        // Close both files, if possible
        try {
            input1.close();
            input2.close();
        }
        catch (IOException e) {
            System.err.println("Cannot close file.\n" + e.getMessage());
        }
    }


    public double getTransitionScore(String previous, String current, String next){
        // check if part of speech 1 can transition to part of speech 2 according to training files

        if(pairToNextMap.get(previous).containsKey(current)&&pairToNextMap.get(previous).get(current).containsKey(next)) {
            // how many times does tag 2 comes after tag 1 compared to how many times tag 1 appears at all

            double totalPreviousTagsSeen = pairsFrequency.get(previous).get(current);
            double transitionProbability = ((double)pairToNextMap.get(previous).get(current).get(next))/(totalPreviousTagsSeen);
            return Math.log(transitionProbability);
        }
        else if(pairsFrequency.get(current).containsKey(next)){// check if part of speech 1 can transition to part of speech 2 according to training files

            return Math.log(((double)(pairsFrequency.get(current).get(next))) / ((double) (tagFrequency.get(current))));
        }
        else{
            // no chance that tag 2 comes after tag 1, according to training files
            return -100.0;
        }
    }


    /**
     * @param word whose part of speech must be determined
     * @param tag possible part of speech of the word
     * @return probability that a given word in a sentence is of type tag
     */
    public double getObservationProbability(String word, String tag){
        if(observationsMap.containsKey(word)&&observationsMap.get(word).containsKey(tag)){
            double timesTagWasSeen = tagFrequency.get(tag);
            return Math.log((double)(observationsMap.get(word).get(tag))/(timesTagWasSeen));
        }
        else{
            return -100.0;
        }
    }

    /**
     * Once the program has been trained with other files, determine tags on the sentence of a given file
     * @param fileName to "decode" or tag
     * @param resultFileName path to send resulting tags
     */
    public void determineTags(String fileName, String resultFileName){
        BufferedReader input;
        BufferedWriter output;

        // Open the file, if possible
        try {
            input = new BufferedReader(new FileReader(fileName));
        }
        catch (FileNotFoundException e) {
            System.err.println("Cannot open file.\n" + e.getMessage());
            return;
        }
        // Open the file, if possible
        try{
            output = new BufferedWriter(new FileWriter(resultFileName));
        }
        catch(IOException e){
            System.err.println("Cannot open result file.\n" + e.getMessage());
            return;
        }

        try {
            // read every line in a file, corresponding to a sentence
            String sentence;
            while ((sentence = input.readLine()) != null) {
                // calls method that determines tags on a given sentence
                String decodedTags = ViterbiDecoding(sentence);
                // write to output file
                output.write(decodedTags+"\n");
            }
        }
        catch (IOException e) {
            System.err.println("IO error while reading.\n" + e.getMessage());
        }

        // Close both files, if possible
        try {
            input.close();
        }
        catch (IOException e) {
            System.err.println("Cannot close file.\n" + e.getMessage());
        }

        // Close the file, if possible
        try {
            output.close();
        }
        catch (IOException e) {
            System.err.println("Cannot close file.\n" + e.getMessage());
        }
    }

    /**
     * @param sentence a string of words to be tagged
     * @return a string with a tag for each corresponding word in the sentence
     */

    public String ViterbiDecoding(String sentence) {
//List that contains at every index a map with all possible tags the word at that index could have and each tag's probability as the value
        List<Map<String, Map<String, String>>> backTrace = new ArrayList<>();

        // add first state, start of the sentence #
        // current state keeps track of the element at the last index of states list, initially # start
        Map<String, Map<String, Double>> currentState = new HashMap<>();
        currentState.put("#", new HashMap<>());
        currentState.get("#").put("@", 0.0);

        Map<String, Map<String, String>> currentTrace = new HashMap<>();
        currentTrace.put("#", new HashMap<>());
        currentTrace.get("#").put("@", "@");
        backTrace.add(currentTrace);

        sentence = sentence.toLowerCase();
        String[] words = sentence.split(" ");

        for (String word : words) {

            // for the next word in the sentence, find all possible states with their corresponding probabilities
            Map<String, Map<String, Double>> nextState = new HashMap<>();
            Map<String, Map<String, String>> nextTrace = new HashMap<>();

            // for each of the tags in current determine the tags where they can transition to
            for (String current : currentState.keySet()) {

                for(String previous : currentTrace.get(current).keySet()) {

                    // score in the current state, used to compute next score
                    double currentScore = currentState.get(current).get(previous);


                    if (pairToNextMap.get(previous).containsKey(current)) {
                        // loop through all next tags where the current state can transition to

                        for (String next : pairsFrequency.get(current).keySet()) {

                            double transScore = getTransitionScore(previous, current, next);
                            // probability that the word is of type next tag
                            double observation = getObservationProbability(word, next);
                            // compute next score upon transition and observation
                            double nextScore = currentScore+transScore+observation;

                            // only consider next tag if next state had not seen it before or if it's probability,
                            // coming from a different current tag, is greater than the probability seen before.
                            if (!nextState.containsKey(next)) {
                                // put inside the map at next state with best probability seen so far, or only probability
                                nextState.put(next, new HashMap<>());
                                nextState.get(next).put(current, nextScore);
                                // backtrack: next comes from current
                                nextTrace.put(next, new HashMap<>());
                                nextTrace.get(next).put(current, previous);
                            }
                            else if(!nextState.get(next).containsKey(current)||nextScore > nextState.get(next).get(current)){
                                nextState.get(next).put(current, nextScore);

                                nextTrace.get(next).put(current, previous);
                            }
                        }
                    } else if (pairsFrequency.containsKey(current)) {

                        for (String next : pairsFrequency.get(current).keySet()) {
                            // probability of transitioning from current tag to next tag
                            double transScore = getTransitionScore(previous, current, next);
                            // probability that the word is of type next tag
                            double observation = getObservationProbability(word, next);
                            // compute next score upon transition and observation
                            double nextScore = currentScore + transScore + observation;

                            // only consider next tag if next state had not seen it before or if it's probability,
                            // coming from a different current tag, is greater than the probability seen before.
                            if (!nextState.containsKey(next)) {
                                // put inside the map at next state with best probability seen so far, or only probability
                                nextState.put(next, new HashMap<>());
                                nextState.get(next).put(current, nextScore);
                                // backtrack: next comes from current
                                nextTrace.put(next, new HashMap<>());
                                nextTrace.get(next).put(current, previous);
                            }
                            else if(!nextState.get(next).containsKey(current)||nextScore > nextState.get(next).get(current)){
                                nextState.get(next).put(current, nextScore);

                                nextTrace.get(next).put(current, previous);
                            }
                        }
                    }
                }
            }

            backTrace.add(nextTrace);

            // advance through the words by changing pointer to current
            currentState = nextState;
            currentTrace = nextTrace;
        }

        /*
         * Back Tracking on entire sentence:
         *
         * 1st find out what the best probability is on the last state
         * Then, loop through all states and find out which path produced the best probability last state
         */
        // on last state, we don't know what the tag with the best probability is
        String bestNextTag = "";
        String bestCurrentTag = "";


        // loop through last state to find best probability tag
        for (String next : currentState.keySet()) {
            for(String current: currentState.get(next).keySet()){
                try {
                    // if we find one that's better, update best probability
                    if (currentState.get(next).get(current) > currentState.get(bestNextTag).get(bestCurrentTag)) {
                        bestNextTag = next;
                        bestCurrentTag = current;
                    }
                } catch (NullPointerException e) {
                    bestNextTag = next;
                    bestCurrentTag = current;
                }
            }
        }


        /*
         * for observation i, for each state, what was the previous state at observation i-1 that produced the
         * best score upon transition and observation
         */

        String decodedTags = ""; // list with tags

        // start at best tag found previously, march down to first state
        String nextTag = bestNextTag;
        String currentTag = bestCurrentTag;
        decodedTags = " " + nextTag;

        // go through all states, starting from last one
        for (int i = backTrace.size() - 1; i > 1; i--) {
            // add to string
            decodedTags = " " + currentTag + decodedTags;

            String temp = currentTag;
            currentTag = backTrace.get(i).get(nextTag).get(currentTag);
            nextTag = temp;

        }

        decodedTags = decodedTags.substring(1);

        return decodedTags;
    }

    /**
     * @param sentence a string of words to be tagged
     * @return a string with a tag for each corresponding word in the sentence
     */

    public ArrayList<Map<String, String>> predictNext(String sentence) {
        //List that contains at every index a map with all possible tags the word at that index could have and each tag's probability as the value
        List<Map<String, Map<String, String>>> backTrace = new ArrayList<>();

        // add first state, start of the sentence #
        // current state keeps track of the element at the last index of states list, initially # start
        Map<String, Map<String, Double>> currentState = new HashMap<>();
        currentState.put("#", new HashMap<>());
        currentState.get("#").put("@", 0.0);

        Map<String, Map<String, String>> currentTrace = new HashMap<>();
        currentTrace.put("#", new HashMap<>());
        currentTrace.get("#").put("@", "@");
        backTrace.add(currentTrace);

        sentence = sentence.toLowerCase();
        String[] words = sentence.split(" ");

        for (String word : words) {

            // for the next word in the sentence, find all possible states with their corresponding probabilities
            Map<String, Map<String, Double>> nextState = new HashMap<>();
            Map<String, Map<String, String>> nextTrace = new HashMap<>();

            // for each of the tags in current determine the tags where they can transition to
            for (String current : currentState.keySet()) {

                for(String previous : currentTrace.get(current).keySet()) {

                    // score in the current state, used to compute next score
                    double currentScore = currentState.get(current).get(previous);


                    if (pairToNextMap.get(previous).containsKey(current)) {
                        // loop through all next tags where the current state can transition to

                        for (String next : pairsFrequency.get(current).keySet()) {

                            double transScore = getTransitionScore(previous, current, next);
                            // probability that the word is of type next tag
                            double observation = getObservationProbability(word, next);
                            // compute next score upon transition and observation
                            double nextScore = currentScore+transScore+observation;

                            // only consider next tag if next state had not seen it before or if it's probability,
                            // coming from a different current tag, is greater than the probability seen before.
                            if (!nextState.containsKey(next)) {
                                // put inside the map at next state with best probability seen so far, or only probability
                                nextState.put(next, new HashMap<>());
                                nextState.get(next).put(current, nextScore);
                                // backtrack: next comes from current
                                nextTrace.put(next, new HashMap<>());
                                nextTrace.get(next).put(current, previous);
                            }
                            else if(!nextState.get(next).containsKey(current)||nextScore > nextState.get(next).get(current)){
                                nextState.get(next).put(current, nextScore);

                                nextTrace.get(next).put(current, previous);
                            }
                        }
                    } else if (pairsFrequency.containsKey(current)) {

                        for (String next : pairsFrequency.get(current).keySet()) {
                            // probability of transitioning from current tag to next tag
                            double transScore = getTransitionScore(previous, current, next);
                            // probability that the word is of type next tag
                            double observation = getObservationProbability(word, next);
                            // compute next score upon transition and observation
                            double nextScore = currentScore + transScore + observation;

                            // only consider next tag if next state had not seen it before or if it's probability,
                            // coming from a different current tag, is greater than the probability seen before.
                            if (!nextState.containsKey(next)) {
                                // put inside the map at next state with best probability seen so far, or only probability
                                nextState.put(next, new HashMap<>());
                                nextState.get(next).put(current, nextScore);
                                // backtrack: next comes from current
                                nextTrace.put(next, new HashMap<>());
                                nextTrace.get(next).put(current, previous);
                            }
                            else if(!nextState.get(next).containsKey(current)||nextScore > nextState.get(next).get(current)){
                                nextState.get(next).put(current, nextScore);

                                nextTrace.get(next).put(current, previous);
                            }
                        }
                    }
                }
            }

            backTrace.add(nextTrace);

            // advance through the words by changing pointer to current
            currentState = nextState;
            currentTrace = nextTrace;
        }

        // keep a map of the best words we can predict and their scores
        Map<String, Double> predictedWords = new HashMap<>();
        // keep a map with the tags used for this predicted words
        Map<String, String> predictedWordsTag = new HashMap<>();

        // loop through last state to find best prediction word
        for (String current : currentState.keySet()) {

            for(String previous : currentTrace.get(current).keySet()) {
                // get the current score
                double currentScore = currentState.get(current).get(previous);

                if(pairsFrequency.containsKey(current)) {
                    // for every possible next tag we could have
                    for (String next : pairsFrequency.get(current).keySet()) {

                        // for every word we have seen so far
                        for (String predictedWord : observationsMap.keySet()) {

                            // if this word has been found withe the tag we are analysing

                            if (observationsMap.get(predictedWord).containsKey(next)) {

                                // calculate it's current score
                                double transScore = getTransitionScore(previous, current, next);

                                // it's observational probability should be really low
                                double observation = getObservationProbability(predictedWord, next);
                                double nextScore = currentScore + transScore + observation;

                                // if we have already seen this word before
                                if (predictedWords.containsKey(predictedWord)) {
                                    // if the tag we are using for the word, calculates a bigger score, replace it in the maps
                                    if (predictedWords.get(predictedWord) < nextScore) {
                                        predictedWords.put(predictedWord, nextScore);
                                        predictedWordsTag.put(predictedWord, next);
                                    }
                                }
                                // if we haven't seen the word before
                                else {
                                    // just place it in the map
                                    predictedWords.put(predictedWord, nextScore);
                                    predictedWordsTag.put(predictedWord, next);
                                }
                            }
                        }
                    }
                }
                else{
                    System.out.println(current);
                }
            }
        }

        // create a priority queue to see which predicted words give out the best scores
        PriorityQueue<String> pq = new PriorityQueue<>(new Comparator<String>() {
            @Override
            // add them to the pq depending on their scores (decreasing order (largest at index 0)
            public int compare(String o1, String o2) {
                if (predictedWords.get(o1) < predictedWords.get(o2)) {return 1;}
                else if (predictedWords.get(o1) > predictedWords.get(o2)) {return -1; }
                else { return 0;}
            }
        });

        // add all of the predicted words into the priority queue
        for (String predictions: predictedWords.keySet()) {
            pq.add(predictions);
        }

        // make a array list that holds a map of the top 30 words and their tags
        ArrayList<Map<String, String>> result = new ArrayList<>();

        // if priority queue is smaller than 30 words, only add those
        int maxNumber = Math.min(30, pq.size());

        for (int i = 0; i < maxNumber; i++) {
            result.add(i, new HashMap<>());
            String word = pq.remove();
            if (!word.equals(words[words.length-1])) {
                result.get(i).put(word, predictedWordsTag.get(word));
            }
        }

        return result;
    }


    /**
     *  Method that allows user to input a sentence and prints tags for each word
     */
    public void inputConsole() {

        System.out.println("\nInstructions: input a sentence, we'll let you know what each word's parts of speech is");
        System.out.println("Please make sure that when using punctuation, you separate it from the word");
        System.out.println("For example, 'Hello , hope you have a nice day !'");


        // allow user input
        Scanner scan = new Scanner(System.in);
        System.out.println("\nEnter your sentence: ");
        String s = scan.nextLine();

        // while the user does not want to quit
        while (!s.equals("q")) {

            if(s.startsWith("t ")) {
                // split the tags and words into a String Array
                String sentence = s.substring(2);

                String[] tags = ViterbiDecoding(s).split(" ");
                String[] words = s.split(" ");

                // create a resulting string
                String result = "";

                // for every word and tag
                for (int i = 0; i < words.length; i++) {
                    // add it to the result string
                    result += words[i] + "/" + tags[i] + " ";
                }

                System.out.println(result);
            }
            else if(s.startsWith("p ")){
                // split the tags and words into a String Array
                String sentence = s.substring(2);

                System.out.println(predictNext(sentence));
            }
            // ask for more user input
            scan = new Scanner(System.in);
            System.out.println("\nEnter your sentence: ");
            s = scan.nextLine();
        }

        // if the user quit the game
        System.out.println("Good Bye!");
    }

    /**
     * @param fileName1 tags file 1
     * @param fileName2 tags file 2
     * @return a string describing the number of different tags in both files and the total number of words
     */
    public String discrepancies(String fileName1, String fileName2){
        BufferedReader input1;
        BufferedReader input2;

        String result = "";
        int numOfDiscrepancies = 0;
        int totalNumber = 0;

        // Open the file, if possible
        try {
            input1 = new BufferedReader(new FileReader(fileName1));
            input2 = new BufferedReader(new FileReader(fileName2));
        }
        catch (FileNotFoundException e) {
            System.err.println("Cannot open file.\n" + e.getMessage());
            return result;
        }
        // read files if possible
        try {

            // read every line in both file
            String sentenceTags1;
            String sentenceTags2;
            while (((sentenceTags1 = input1.readLine()) != null)&&((sentenceTags2 = input2.readLine()) != null)) {

                // get the tags in every file
                String[] firstTags =  sentenceTags1.split(" ");
                String[] secondTags =  sentenceTags2.split(" ");

                // loop through tags and if you find one that doesn't match, increase the discrepancies count
                int index = 0;
                while(index<Math.min(firstTags.length, secondTags.length)){
                    totalNumber += 1;
                    if(!(firstTags[index].equals(secondTags[index]))){
                        numOfDiscrepancies += 1;
                    }
                    index++;
                }

                // make sure they both have the same number of tags
                if(firstTags.length<secondTags.length){
                    numOfDiscrepancies += secondTags.length-firstTags.length;
                }
                if(firstTags.length>secondTags.length){
                    numOfDiscrepancies += firstTags.length-secondTags.length;
                }

            }

        }
        catch (IOException e) {
            System.err.println("IO error while reading.\n" + e.getMessage());
        }

        // Close both files, if possible
        try {
            input1.close();
            input2.close();
        }
        catch (IOException e) {
            System.err.println("Cannot close file.\n" + e.getMessage());
        }

        result = "Found "+numOfDiscrepancies+" discrepancies out of "+totalNumber+" total words.";
        return result;
    }
//
//    public static void main(String[] args) {
//        // used for debugging purposes
//        if (debugFlag) {
//            // training the machine with simple-train
//            String simpleTrainTags = "PS5/simple-train-tags.txt";
//            String simpleTrainSentences = "PS5/simple-train-sentences.txt";
//
//            SudiEC test = new SudiEC();
//            test.trainMachine(simpleTrainSentences, simpleTrainTags);
//            System.out.println(test.pairToNextMap);
//            System.out.println(test.observationsMap);
//        }
//
//        if(test1){
//            // training the machine with test-1
//            String simpleTrainTags = "PS5/testFile1-tags.txt";
//            String simpleTrainSentences = "PS5/testFile1-sentences.txt";
//            String result = "PS5/testFile1-result.txt";
//
//            SudiEC test = new SudiEC();
//            test.trainMachine(simpleTrainSentences, simpleTrainTags);
//            test.determineTags(simpleTrainSentences, result);
//            System.out.println(test.discrepancies(result, result));
//        }
//
//        if(shortenedBrown){
//            String simpleTrainTags = "PS5/brown-tags-shortened.txt";
//            String simpleTrainSentences = "PS5/brown-sentences-shortened.txt";
//            String test = "PS5/brown-test-shortened.txt";
//            String answers = "PS5/brown-testresult-shortened.txt";
//            String result = "PS5/brown-shortened-result.txt";
//
//            SudiEC shortenedBrownTest = new SudiEC();
//            shortenedBrownTest.trainMachine(simpleTrainSentences, simpleTrainTags);
//            shortenedBrownTest.determineTags(test, result);
//            System.out.println(shortenedBrownTest.discrepancies(answers, result));
//        }
//
//        if (simpleTest) {
//            // training the machine with simple-train
//
//            String simpleTrainTags = "PS5/simple-train-tags.txt";
//            String simpleTrainSentences = "PS5/simple-train-sentences.txt";
//
//            SudiEC simpleTrainTest = new SudiEC();
//            simpleTrainTest.trainMachine(simpleTrainSentences, simpleTrainTags);
//
//            String input = "PS5/simple-test-sentences.txt";
//            String answersFile = "PS5/simple-test-tags.txt";
//            String result = "PS5/simple-test-result2.txt";
//
//            simpleTrainTest.determineTags(input, result);
//            System.out.println(simpleTrainTest.discrepancies(answersFile, result));
//
//            System.out.println(simpleTrainTest.pairsFrequency);
//
//            simpleTrainTest.inputConsole();
//        }
//
//        if (brownTest) {
//            // training the machine with brown-train
//            String brownTrainTags = "PS5/brown-train-tags.txt";
//            String brownTrainSentences = "PS5/brown-train-sentences.txt";
//
//            SudiEC brownTrainTest = new SudiEC();
//            brownTrainTest.trainMachine(brownTrainSentences, brownTrainTags);
//
//            String input = "PS5/brown-test-sentences.txt";
//            String result = "PS5/brown-test-result2.txt";
//            String answersFile = "PS5/brown-test-tags.txt";
//
//            brownTrainTest.determineTags(input, result);
//            System.out.println(brownTrainTest.discrepancies(answersFile, result));
//            brownTrainTest.inputConsole();
//        }
//
//    }
}