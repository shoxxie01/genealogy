package org.umcspro.genealogy;


import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//Zadanie 1.
// Napisz klasę Person, w której znajdować będą się dane odpowiadające wierszowi pliku.
// Na tym etapie pomiń wczytywanie rodziców. Napisz metodę wytwórczą fromCsvLine()
// klasy Person przyjmującą jako argument linię opisanego pliku.
public class Person implements Serializable{
    private final String name;
    private final LocalDate birthDate;
    private final LocalDate deathDate;
    private final List<Person> parents;

    public Person(String name, LocalDate birthDate, LocalDate deathDate) {
        this.name = name;
        this.birthDate = birthDate;
        this.deathDate = deathDate;
        this.parents = new ArrayList<>();
    }

    public static Person fromCsvLine(String csvLine){
        String[] line= csvLine.split(",",-1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        LocalDate birthDate = LocalDate.parse(line[1],formatter);
        LocalDate deathDate = line[2].equals("")?null : LocalDate.parse(line[2],formatter);
        return new Person(line[0], birthDate, deathDate);
    }
    //Zadanie 2.
    //Napisz metodę fromCsv(), która przyjmie ścieżkę do pliku i zwróci listę obiektów typu Person.
    public static List<Person> fromCsv(String path) {
        List<Person> people = new ArrayList<>();
        //<----------------------------------------------------------------zad5

        Map<String, PersonWithParentsNames> mapPersonWithParentNames = new HashMap<>();
        //<----------------------------------------------------------------zad5

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                // <---------------------------------zad2
                //Person person = Person.fromCsvLine(line);
                   // person.validateLifeSpan();
                   // person.validateAmbiguous(people);

                //people.add(person);
                // <---------------------------------zad2
                //<---------------------------------zad5
                PersonWithParentsNames personWithNames = PersonWithParentsNames.fromCsvLine(line);
                personWithNames.getPerson().validateLifeSpan();
                personWithNames.getPerson().validateAmbiguous(people);

                Person person = personWithNames.getPerson();
                people.add(person);
                mapPersonWithParentNames.put(person.name,personWithNames);
                //<-------------------------------- zad5

            }
            PersonWithParentsNames.linkRelatives(mapPersonWithParentNames);
            try {
                for(Person person: people) {
                    System.out.println("Sprwadzam");
                    person.validateParentingAge();
                }
            }
            catch(ParentingAgeException exception) {
                Scanner scanner = new Scanner(System.in);
                System.out.println(exception.getMessage());
                System.out.println("Please confirm [Y/N]:");
                String response = scanner.nextLine();
                if(!response.equals("Y") && !response.equals("y"))
                    people.remove(exception.person);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }catch (NegativeLifespanException | AmbiguousPersonException e) {
            System.err.println(e.getMessage());
        }

        return people;
    }

    private void validateAmbiguous(List<Person> people) throws AmbiguousPersonException {
        for (Person person : people) {
            if (person.getName().equals(getName())) {
                throw new AmbiguousPersonException(person);
            }
        }
    }

    private void validateLifeSpan() throws NegativeLifespanException{
        if (deathDate != null && deathDate.isBefore(birthDate)){
            throw new NegativeLifespanException(this);
        }
    }

    private void validateParentingAge() throws ParentingAgeException {
        for(Person parent: parents) {
            if (birthDate.isBefore(parent.birthDate.plusYears(15)) || (parent.deathDate != null && birthDate.isAfter(parent.deathDate)))
                throw new ParentingAgeException(this, parent);
        }
    }


    public String getName() {
        return name;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public LocalDate getDeathDate() {
        return deathDate;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", birthDate=" + birthDate +
                ", deathDate=" + deathDate +
                ", parents=" + parents +
                '}';
    }

    public void addParent(Person person) {
        parents.add(person);
    }


    //<----------------------------------------------------------------------------------------zad7

    public static void toBinaryFile(List<Person> people, String filename) throws IOException {
        try (
                FileOutputStream fos = new FileOutputStream(filename);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
        ) {
            oos.writeObject(people);
        }
    }

    public static List<Person> fromBinaryFile(String filename) throws IOException, ClassNotFoundException {
        try (
                FileInputStream fis = new FileInputStream(filename);
                ObjectInputStream ois = new ObjectInputStream(fis);
        ) {
            return (List<Person>) ois.readObject();
        }
    }

    public String generateUML(){
        StringBuilder sb =new StringBuilder();

        Function<Person, String> deleteSpaces = p -> p.getName().replaceAll(" ","");
        Function<Person, String> addObject = p -> "object " + deleteSpaces.apply(p);
        String nameSurname = deleteSpaces.apply(this);

        sb
                .append("@startuml\n")
                .append(addObject.apply(this));

        if(!parents.isEmpty()){
            sb.append(parents
                    .stream()
                    .map(p -> "\n" + addObject.apply(p) + "\n" + deleteSpaces.apply(p) + " <-- " +nameSurname + "\n")
                    .collect(Collectors.joining()));
        }
        sb.append("\n@enduml");
        return sb.toString();
    }

    public static String generateUML(List<Person> people){
        StringBuilder sb = new StringBuilder();
        Function<Person, String> deleteSpaces = p -> p.getName().replaceAll(" ","");
        Function<Person, String> addObject = p -> "object " + deleteSpaces.apply(p);
        sb.append("@startuml");
        sb.append(people
                .stream()
                .map(p -> "\n" + addObject.apply(p))
                .collect(Collectors.joining()));
        sb.append("\n");
        sb.append(people
                .stream()
                .flatMap(person -> person.parents.isEmpty() ? Stream.empty() :
                        person.parents.stream().map(p -> deleteSpaces.apply(p) + " <-- " + deleteSpaces.apply(person) + "\n"))
                .collect(Collectors.joining()));
        sb.append("\n@enduml");
        return sb.toString();
    }

    public static List<Person> filterByName(List<Person> people, String substring){
        return people
                .stream()
                .filter(p -> p.getName().contains(substring))
                .collect(Collectors.toList());
    }

    public static  List<Person> sortByName(List<Person> people){
        Function<Person, Long> birthDateToLong = p -> p.birthDate.toEpochDay();

        return people
                .stream()
                .sorted(Comparator.comparingLong(birthDateToLong::apply)).toList();
    }

    //<----------------------------------------------------------------------------------------zad7

}
