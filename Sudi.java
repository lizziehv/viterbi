import java.io.*;
import java.util.*;

/**
 * @author Lizzie Hernandez Videa
 * Partner: María Paula Mora
 * Monday November 4th
 *
 * Program that can be trained to identify patterns in word tagging and then produce the corresponding sequence of tags
 */

public class Sudi {
    public static boolean debugFlag = false;
    public static boolean simpleTest = false;
    public static boolean brownTest = false;
    public static boolean test1 = false;
    public static boolean shortenedBrown = false;


    // graph with tags as vertices and transitions frequencies between tags as edges
    public Graph<String, Integer> tagProbabilityGraph;

    // map matching a tag to the number of times it has been seen while training
    public Map<String, Integer> sentenceNumPassingThrough;

    // map matching a word with all its possible tags and the amount of times it has been seen with that tag
    public Map<String, Map<String, Integer>> observationsMap;

    /**
     *  Constructor no parameters
     */
    public Sudi(){
        tagProbabilityGraph = new AdjacencyMapGraph<>();
        sentenceNumPassingThrough = new HashMap<>();
        observationsMap = new HashMap<>();
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
            // insert a start vertex indicating the beginning of a sentence
            tagProbabilityGraph.insertVertex("#");
            sentenceNumPassingThrough.put("#", 0);

            String words;       // words read from original file
            String states;      // tags read from parts of speech file
            while ((words = input1.readLine())!= null&&(states = input2.readLine())!= null) {
                String[] partsOfSpeech = states.split(" ");
                String[] correspondingWords = words.split(" ");

                // for every sentence start with the start of sentence indicator (#)
                String current = "#";
                // increase # for the number of sentences seen
                sentenceNumPassingThrough.put(current, sentenceNumPassingThrough.get(current)+1);

                // loop through words and corresponding parts of speech in a sentence
                int i = 0;
                while(i<partsOfSpeech.length){
                    // get a word and its corresponding tag
                    String tag = partsOfSpeech[i];
                    String word = correspondingWords[i].toLowerCase();

                    // if the tag has never been seen before, add it to the graph
                    if(!tagProbabilityGraph.hasVertex(tag)){
                        tagProbabilityGraph.insertVertex(tag);
                        sentenceNumPassingThrough.put(tag, 0);
                    }

                    // increment the count of times a vertex has been seen by one
                    sentenceNumPassingThrough.put(tag, sentenceNumPassingThrough.get(tag)+1);

                    // if there wasn't a connection between current tag and next tag, add a connection with frequency 1
                    if(!tagProbabilityGraph.hasEdge(current, tag)){
                        tagProbabilityGraph.insertDirected(current, tag, 1);
                    }
                    // else increment the frequency of transitioning from word to another by 1
                    else {
                        int label = tagProbabilityGraph.getLabel(current, tag) + 1;
                        tagProbabilityGraph.removeDirected(current, tag);
                        tagProbabilityGraph.insertDirected(current, tag, label);
                    }

                    // if the word hadn't already been seen, add it to observations
                    if(!observationsMap.containsKey(word)){
                        observationsMap.put(word, new HashMap<>());
                    }
                    // if the word hadn't already been seen with this tag, add the taag to it's map of possible tags
                    if(!observationsMap.get(word).containsKey(tag)){
                        observationsMap.get(word).put(tag, 0);
                    }
                    // increment the frequency by which the word has been seen with this tag by 1
                    observationsMap.get(word).put(tag, observationsMap.get(word).get(tag) + 1);

                    // advance through other words in the sentence
                    current = tag;
                    i ++;

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

    /**
     *  What is the probability that following tag1 is tag2?
     * @param tag1 coming from
     * @param tag2 going to
     * @return probability that tag2 follows tag1
     */
    public double getTransitionProbability(String tag1, String tag2){
        // check if part of speech 1 can transition to part of speech 2 according to training files
        if(tagProbabilityGraph.hasEdge(tag1, tag2)) {
            // how many times does tag 2 comes after tag 1 compared to how many times tag 1 appears at all
            return Math.log(((double)(tagProbabilityGraph.getLabel(tag1, tag2))) / ((double) (sentenceNumPassingThrough.get(tag1))));
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
            return Math.log((double)(observationsMap.get(word).get(tag))/(double)(sentenceNumPassingThrough.get(tag)));
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

    public String ViterbiDecoding(String sentence){
        //List that contains at every index a map with all possible tags the word at that index could have and each tag's probability as the value
        List<Map<String, Double>> states = new ArrayList<>();
        //List that contains at every index a map with all possible tags the word at that index+1 could have had and where they came from
        List<Map<String, String>> backTrack = new ArrayList<>();

        // add first state, start of the sentence #
        // current state keeps track of the element at the last index of states list, initially # start
        Map<String, Double> currentState = new HashMap<>();
        currentState.put("#", 0.0);
        states.add(currentState);

        sentence = sentence.toLowerCase();

        String[] words = sentence.split(" ");
        for (String word : words) {

            // for the next word in the sentence, find all possible states with their corresponding probabilities
            Map<String, Double> nextState = new HashMap<>();
            // keep track of the current state tags where all tags in this next state came from
            Map<String, String> stateBackTrack = new HashMap<>();

            // for each of the tags in current determine the tags where they can transition to
            for (String current : currentState.keySet()) {

                // score in the current state, used to compute next score
                double currentScore = currentState.get(current);

                // loop through all next tags where the current state can transition to
                for (String next : tagProbabilityGraph.outNeighbors(current)) {

                    // probability of transitioning from current tag to next tag
                    double transScore = getTransitionProbability(current, next);
                    // probability that the word is of type next tag
                    double observation = getObservationProbability(word, next);
                    // compute next score upon transition and observation
                    double nextScore = currentScore + transScore + observation;

                    // only consider next tag if next state had not seen it before or if it's probability,
                    // coming from a different current tag, is greater than the probability seen before.
                    if (!nextState.containsKey(next)||nextScore > nextState.get(next)){
                        // put inside the map at next state with best probability seen so far, or only probability
                        nextState.put(next, nextScore);
                        // backtrack: next comes from current
                        stateBackTrack.put(next, current);
                    }
                }
            }

            // consider the entire state, and the entire state's backtracking
            states.add(nextState);
            backTrack.add(stateBackTrack);

            // advance through the words by changing pointer to current
            currentState = nextState;
        }

        /*
         * Back Tracking on entire sentence:
         *
         * 1st find out what the best probability is on the last state
         * Then, loop through all states and find out which path produced the best probability last state
         */
        // on last state, we don't know what the tag with the best probability is
        String bestProbabilityTag = "";

        // loop through last state to find best probability tag
        for (String tag : currentState.keySet()) {
            // if first time, instantiate to first tag in last state (try, catch)
            try {
                // if we find one that's better, update best probability
                if (currentState.get(tag) > currentState.get(bestProbabilityTag)) {
                    bestProbabilityTag = tag;
                }
            } catch (NullPointerException e) {
                // first time--> start with first possible tag
                bestProbabilityTag = tag;
            }
        }

        /*
         * for observation i, for each state, what was the previous state at observation i-1 that produced the
         * best score upon transition and observation
         */

        String decodedTags = ""; // list with tags

        // start at best tag found previously, march down to first state
        String nextTag = bestProbabilityTag;

        // go through all states, starting from last one
        for (int i = states.size() - 1; i > 0; i--) {
            // add to string
            decodedTags = " " + nextTag + decodedTags;
            nextTag = backTrack.get(i-1).get(nextTag);
        }

        decodedTags = decodedTags.substring(1);

        return decodedTags;
    }

    /**
     * EXTRA CREDIT
     * Using our model predictively to identify what the best next word would be to continue a sentence
     * @param sentence      // an unfinished sentence
     * @return              // the top 30 best words to complete the sentence
     */
    public ArrayList<Map<String, String>> predictNext(String sentence) {
        // An array list, the list will be ordered depending on the word we are currently in
        // Inside the entry, will be a map for the word, with a Key being the potential Part of Speech and the Value being their currScore

        // add the start node to the array
        Map<String, Double> currentState = new HashMap<>();
        currentState.put("#", 0.0);


        // Change the entire sentence to lower case letters
        sentence = sentence.toLowerCase();
        String[] words = sentence.split(" ");

        // follow a similar process of the viterbi encoding
        // for every word in the sentence
        for (String word : words) {

            // keep track of the next states and their current scores
            Map<String, Double> nextState = new HashMap<>();

            // for every current state we have in the key set
            for (String current : currentState.keySet()) {

                // get their current score
                double currentScore = currentState.get(current);

                // loop through with every part of speech we have studied
                for (String next : tagProbabilityGraph.outNeighbors(current)) {

                    // calculate it's current score
                    double transScore = getTransitionProbability(current, next);

                    // it's observational probability should be really low
                    double observation = getObservationProbability(word, next);
                    double nextScore = currentScore + transScore + observation;

                    // if the we have seen this tag but with a different tag before it
                    if (nextState.containsKey(next)) {
                        if (nextScore > nextState.get(next)) {
                            // this word is better coming from this other current tag, update this score
                            nextState.put(next, nextScore);
                        }
                    }
                    // if we haven't see this tag before
                    else {
                        // just added with the currently calculated score
                        nextState.put(next, nextScore);
                    }
                }
            }

            // these next states will become the current states so that we can calculate the next words next states
            currentState = nextState;
        }

        // now that we have arrived at the end of the sentence, calculate the next more probable word

        // keep a map of the best words we can predict and their scores
        Map<String, Double> predictedWords = new HashMap<>();
        // keep a map with the tags used for this predicted words
        Map<String, String> predictedWordsTag = new HashMap<>();

        // loop through last state to find best prediction word
        for (String current : currentState.keySet()) {

            // get the current score
            double currentScore = currentState.get(current);

            // for every possible next tag we could have
            for (String next : tagProbabilityGraph.outNeighbors(current)) {

                // for every word we have seen so far
                for (String predictedWord : observationsMap.keySet()) {

                    // if this word has been found withe the tag we are analysing

                    if (observationsMap.get(predictedWord).containsKey(next)) {

                        // calculate it's current score
                        double transScore = getTransitionProbability(current, next);

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

        for (int i = 0; i <= maxNumber; i++) {
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

            // split the tags and words into a String Array
            String[] tags = ViterbiDecoding(s).split(" ");
            String[] words = s.split(" ");

            // create a resulting string
            String result = "";

            // for every word and tag
            for (int i = 0; i < words.length; i ++) {
                // add it to the result string
                result += words[i] + "/" + tags[i] + " ";
            }

            System.out.println(result);

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

    public static void main(String[] args) {
        // used for debugging purposes
        if (debugFlag) {
            // training the machine with simple-train
            String simpleTrainTags = "PS5/simple-train-tags.txt";
            String simpleTrainSentences = "PS5/simple-train-sentences.txt";

            Sudi test = new Sudi();
            test.trainMachine(simpleTrainSentences, simpleTrainTags);
            System.out.println(test.tagProbabilityGraph);
            System.out.println(test.observationsMap);
        }

        if(test1){
            // training the machine with test-1
            String simpleTrainTags = "PS5/testFile1-tags.txt";
            String simpleTrainSentences = "PS5/testFile1-sentences.txt";
            String result = "PS5/testFile1-result.txt";

            Sudi test = new Sudi();
            test.trainMachine(simpleTrainSentences, simpleTrainTags);
            System.out.println(test.tagProbabilityGraph);
            System.out.println(test.observationsMap);
            test.determineTags(simpleTrainSentences, result);
            System.out.println(test.discrepancies(simpleTrainTags, result));
        }

        if (simpleTest) {
            // training the machine with simple-train
            String simpleTrainTags = "PS5/simple-train-tags.txt";
            String simpleTrainSentences = "PS5/simple-train-sentences.txt";

            Sudi simpleTrainTest = new Sudi();
            simpleTrainTest.trainMachine(simpleTrainSentences, simpleTrainTags);

            String input = "PS5/simple-test-sentences.txt";
            String answersFile = "PS5/simple-test-tags.txt";
            String result = "PS5/simple-test-result2.txt";
            System.out.println(simpleTrainTest.tagProbabilityGraph);
            System.out.println(simpleTrainTest.observationsMap);
            simpleTrainTest.determineTags(input, result);
            System.out.println(simpleTrainTest.discrepancies(answersFile, result));
            simpleTrainTest.inputConsole();
        }

        if(shortenedBrown){
            String simpleTrainTags = "PS5/brown-tags-shortened.txt";
            String simpleTrainSentences = "PS5/brown-sentences-shortened.txt";
            String test = "PS5/brown-test-shortened.txt";
            String answers = "PS5/brown-testresult-shortened.txt";
            String result = "PS5/brown-shortened-result.txt";

            Sudi shortenedBrownTest = new Sudi();
            shortenedBrownTest.trainMachine(simpleTrainSentences, simpleTrainTags);
            shortenedBrownTest.determineTags(test, result);
            System.out.println(shortenedBrownTest.discrepancies(answers, result));
        }

        if (brownTest) {
            // training the machine with brown-train
            String brownTrainTags = "PS5/brown-train-tags.txt";
            String brownTrainSentences = "PS5/brown-train-sentences.txt";

            Sudi brownTrainTest = new Sudi();
            brownTrainTest.trainMachine(brownTrainSentences, brownTrainTags);

            String input = "PS5/brown-test-sentences.txt";
            String result = "PS5/brown-test-result2.txt";
            String answersFile = "PS5/brown-test-tags.txt";

            brownTrainTest.determineTags(input, result);
            System.out.println(brownTrainTest.discrepancies(answersFile, result));
            brownTrainTest.inputConsole();
        }
    }
}