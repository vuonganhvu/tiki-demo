import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static Map<String, Value> mapInput = new HashMap<>();
    private final static String inputFile = "inputStream.txt";
    private final static String outputFile = "outStream.txt";

    public static void main(String[] args) {

        readFile();

        try {
            Map<String, Double> output = process(mapInput.size());
            writeFile(output, null);

        } catch (CircularDependencyException e) {
            System.out.println(e.message);
            writeFile(null, e.message);
        } catch (Exception e) {
            writeFile(null, "Error system");
        }
    }


    private static Map<String, Double> process(int maxInput) throws CircularDependencyException {

        List<Value> valueList = new ArrayList<>();
        for (Map.Entry<String, Value> entry : mapInput.entrySet()) {
            Value value = entry.getValue();
            valueList.add(value);
        }

        Map<String, Double> mapProcess = new HashMap<>();
        int i = 0;
        while (mapProcess.size() < maxInput && i < maxInput) {
            processValue(valueList.get(i), mapProcess, valueList.get(i).getDependencies());
            i++;
        }

        return mapProcess;
    }

    private static Double processValue(Value value, Map<String, Double> mapProcess, List<String> dependencies) throws CircularDependencyException {

        if (Status.NEW.equals(value.getStatus())) {
            value.setStatus(Status.PROCESS);
            String polishPostfix = value.getPolishPostfix();
            if (isNumber(polishPostfix)) {
                mapProcess.put(value.getKey(), Double.valueOf(polishPostfix));
            } else {
                String[] inputs = polishPostfix.split(" ");
                Double ret = calculator(inputs, mapProcess, dependencies);
                mapProcess.put(value.getKey(), ret);
            }
            value.setStatus(Status.DONE);
        } else if (Status.PROCESS.equals(value.getStatus())) {
            dependencies.remove(value.getKey());
            throw new CircularDependencyException(String.format("Circular dependency between %s and %s detected", value.getKey(), String.join(", ", dependencies)));
        }

        return mapProcess.get(value.getKey());
    }

    private static boolean isNumber(String number) {
        Pattern pattern = Pattern.compile("\\d*");
        Matcher matcher = pattern.matcher(number);
        return matcher.matches();
    }

    private static double calculator(String[] strArr, Map<String, Double> mapProcess, List<String> dependencies) throws CircularDependencyException {
        Stack<Double> operands = new Stack<>();

        for(String str : strArr) {
            if (str.trim().equals("")) {
                continue;
            }

            switch (str) {
                case "+":
                case "-":
                case "*":
                case "/":
                    double right = operands.pop();
                    double left = operands.pop();
                    double value = 0;
                    switch(str) {
                        case "+":
                            value = left + right;
                            break;
                        case "-":
                            value = left - right;
                            break;
                        case "*":
                            value = left * right;
                            break;
                        case "/":
                            value = left / right;
                            break;
                        default:
                            break;
                    }
                    operands.push(value);
                    break;
                default:
                    if(isNumber(str)){
                        operands.push(Double.parseDouble(str));
                    } else {
                        Double d = mapProcess.get(str);
                        if(d != null){
                            operands.push(d);
                        } else {
                            dependencies.add(str);
                            operands.push(processValue(mapInput.get(str), mapProcess, dependencies));
                        }
                    }
                    break;
            }
        }
        return operands.pop();
    }

    private static List<Value> readFile() {
        List<Value> valueList = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile))) {

            //Reading the file
            String currentLine;
            int maxLine = 0;
            List<String> values = new ArrayList<>();
            while ((currentLine = bufferedReader.readLine()) != null) {
                if(maxLine == 0){
                    maxLine = Integer.valueOf(currentLine);
                } else {
                    values.add(currentLine);
                }
            }
            for(int i = 0; i < 2*maxLine; i=i+2) {
                Value value = new Value(values.get(i), values.get(i+1));
                valueList.add(value);
                mapInput.put(value.getKey(), value);
            }
        } catch (FileNotFoundException e) {
            System.out.println("The file " + inputFile + "is not found !");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Problem occurs when reading file !");
            e.printStackTrace();
        }

        return valueList;
    }

    private static void writeFile(Map<String, Double> output, String errorMessage) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            if(errorMessage != null && !errorMessage.isEmpty()) {
                bw.write(errorMessage);
            } else {
                List<String> keySet = new ArrayList<>(output.keySet());
                Collections.sort(keySet);
                for (String key : keySet) {
                    bw.write(key);
                    bw.newLine();
                    bw.write(output.get(key).toString());
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class Value {
        String key;
        String polishPostfix;
        Status status;
        List<String> dependencies;

        Value(String key, String polishPostfix) {
            this.key = key;
            this.polishPostfix = polishPostfix;
            status = Status.NEW;
            dependencies = new ArrayList<>();
        }

        String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        String getPolishPostfix() {
            return polishPostfix;
        }

        public void setPolishPostfix(String polishPostfix) {
            this.polishPostfix = polishPostfix;
        }

        Status getStatus() {
            return status;
        }

        void setStatus(Status status) {
            this.status = status;
        }

        public List<String> getDependencies() {
            return dependencies;
        }
    }

    enum Status {
        NEW,PROCESS,DONE
    }

    static class CircularDependencyException extends Exception {

        private String message;

        CircularDependencyException(String message) {
            this.message = message;
        }
    }
}
