package org.umcspro.genealogy;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Person> people = Person.fromCsv("family.csv");

        String uml = Person.generateUML(people);
        PlantUMLRunner.setPath("plantuml-1.2024.4.jar");
        PlantUMLRunner.generateDiagram(uml,"./", "nowy");
    }
}