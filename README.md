# Financial Portfolio Engine

Application Java de gestion et de valorisation de portefeuille d'actifs financiers multi-classes (actions, cryptomonnaies, obligations).

## 1. Objectif du projet

Le projet permet de suivre un portefeuille compose de differentes classes d'actifs, de calculer sa valeur totale a partir des cours du marche, de simuler l'evolution des prix et de passer des ordres d'achat et de vente avec recalcul du prix de revient unitaire (PRU) et calcul des gains imposables.

L'application demarre en chargeant un fichier de donnees initiales (`assets.csv`) sans objets codes en dur, et propose une interface en ligne de commande interactive.

## 2. Execution de l'application

### Pre-requis
- Java 21 ou version superieure
- Maven 3.9+

### Lancement des tests
```bash
mvn test
```

### Compilation et execution
Pour lancer l'application en mode console :
```bash
mvn clean package
java -jar target/financial-portfolio-engine-0.1.jar
```

## 3. Fonctionnalites principales

- Chargement automatique des actifs depuis un fichier CSV au demarrage
- Consultation des positions avec quantite, prix d'achat, cours du marche, valorisation et plus-value latente
- Visualisation directe de la repartition du portefeuille sous forme de jauge console et tendance des cours
- Passage d'ordres d'achat avec verification du solde en especes et ajustement du PRU
- Passage d'ordres de vente avec controle des quantites detenues et estimation de la taxe sur la plus-value
- Simulation de journees de marche appliquee a l'ensemble des cotations

## 4. Classes principales et organisation

Le code source est organise dans le package `com.junia.portfolio` :

- `model.Asset` : classe abstraite representant un actif financier (`symbol`, `quantity`, `purchasePrice`).
- `model.Stock` : sous-classe representant une action, avec dividende annuel et imposition a la Flat Tax (30%).
- `model.CryptoAsset` : sous-classe representant une cryptomonnaie, avec frais de reseau (gas fee) et fractionnement.
- `model.Bond` : sous-classe representant une obligation, avec taux de coupon et annee de maturite.
- `model.Portfolio` : conteneur de la liste des actifs et du solde en especes (`cashBalance`).
- `model.Order` et `model.OrderType` : structure d'un ordre d'achat ou de vente.
- `service.PortfolioService` : gestion des operations du portefeuille (chargement CSV, calcul de la valorisation globale, execution des ordres).
- `service.MarketDataService` : gestion des cours, historique des prix et simulation de fluctuation quotidienne.
- `service.AnalyticsService` : calcul des plus-values latentes, retour sur investissement et assiette fiscale.
- `utils.AbstractCsvParser<T>` et `utils.AssetCsvParser` : chargement et conversion des lignes CSV en objets metier.
- `utils.FinancialFormatter` : affichage des graphiques consoles (barres d'allocation et sparklines).
- `Application` : point d'entree et boucle principale de l'interface console.

## 5. Decisions techniques

1. Utilisation d'une classe abstraite `Asset` et d'interfaces specifiques :
   Plutot que de tout regrouper dans une classe generique avec un champ d'enum, chaque type d'actif dispose de sa propre classe. Cela permet d'isoler les proprietes specifiques (comme la date de maturite d'une obligation ou les frais de gas d'une crypto) tout en conservant une signature commune pour la valorisation.

2. Se separation stricte du modele et des services :
   Les entites du package `model` ne contiennent que les donnees et les calculs propres a leur etat. Les regles de transaction, la verification des liquidites et la gestion des prix sont deleguees a `PortfolioService` et `MarketDataService`.

3. Traitement des flux de donnees avec les Streams Java :
   Le calcul de la valeur globale du portefeuille et la recherche d'actifs par symbole utilisent l'API `Stream` et `Optional`. Cela evite les boucles avec indices et garantit l'absence de valeurs nulles non traitees.

4. Parsing CSV avec le patron Template Method :
   La classe `AbstractCsvParser<T>` encapsule la lecture du fichier via `Files.lines()` et la gestion des flux d'entree/sortie. La sous-classe `AssetCsvParser` implemente uniquement la conversion d'une ligne en instance de classe.

## 6. Utilisation de l'heritage et du polymorphisme

### Heritage
La classe abstraite `Asset` sert de classe mere aux trois classes concretes `Stock`, `CryptoAsset` et `Bond`. Elle factorise le symbole, la quantite et le prix d'achat initial.

### Polymorphisme
La methode `calculateCurrentValue(double currentPrice)` est declaree dans `Asset` et definie dans chaque classe fille. Dans `PortfolioService`, le calcul de la valorisation globale s'effectue sur une liste d'actifs heterogenes sans aucun test de type (`instanceof`) :

```java
portfolio.getAssets().stream()
    .mapToDouble(asset -> asset.calculateCurrentValue(marketPrice))
    .sum();
```

## 7. Interfaces et classes abstraites

- Classe abstraite `Asset` : definie pour regrouper les comportements communs et imposer la presence de `calculateCurrentValue`.
- Interface `Taxable` : implementee par les actifs generant un gain imposable lors de la cession (`calculateTaxableGain` et `getTaxRate`).
- Interface `Tradeable` : implementee par les actifs pour valider la granularite des ordres (`isOrderSizeValid`, ex: quantite entiere pour les actions, fractionnee pour les cryptos).

## 8. Exceptions personnalisees

Trois exceptions etendant `Exception` ont ete creees dans `com.junia.portfolio.exception` :
- `InsufficientFundsException` : levee lorsqu'un ordre d'achat depasse le montant de liquidites disponible.
- `AssetNotFoundException` : levee lors d'une tentative de vente d'un actif non present dans le portefeuille.
- `InvalidTransactionException` : levee lors d'une saisie invalide (prix ou quantite negative, ou tentative de vendre plus d'unites que celles detenues).

## 9. Chargement des donnees

L'initialisation s'appuie sur `src/main/resources/assets.csv`. Au demarrage, `Application` appelle `PortfolioService.loadAssetsFromCsv()` qui delegue la lecture a `AssetCsvParser`. Le fichier est lu ligne par ligne avec `Files.lines()`, l'en-tete est ignoree via `.skip(1)`, et chaque enregistrement est transforme en objet concret (`Stock`, `CryptoAsset`, `Bond`) ajoute au portefeuille.

## 10. Application des principes SOLID

- Single Responsibility Principle : Chaque classe a une responsabilite unique (`AssetCsvParser` pour la lecture du fichier, `MarketDataService` pour les cours, `PortfolioService` pour les regles de portefeuille).
- Open/Closed Principle : Pour integrer une nouvelle classe d'actif (par exemple `RealEstateAsset`), il suffit d'etendre `Asset` sans modifier les methodes de calcul existantes dans `PortfolioService`.
- Liskov Substitution Principle : N'importe quelle instance de `Stock`, `CryptoAsset` ou `Bond` peut etre manipulee au travers d'une reference de type `Asset` sans alterer le fonctionnement du portefeuille.
- Interface Segregation Principle : Les interfaces `Taxable` et `Tradeable` sont courtes et separees au lieu d'une unique interface chargee de methodes inutiles.
- Dependency Inversion Principle : `PortfolioService` recoit ses dependances (`Portfolio` et `AssetCsvParser`) par constructeur.

## 11. Tests unitaires

Les tests unitaires sont ecrits avec JUnit 5 et se trouvent dans `src/test/java/com/junia/portfolio` :
- `model.AssetTest` : validation du calcul de valeur pour chaque actif et conformite des interfaces `Taxable` et `Tradeable`.
- `utils.AssetCsvParserTest` : verification du chargement des actifs depuis le fichier CSV.
- `utils.FinancialFormatterTest` : verification du rendu des barres d'allocation et sparklines.
- `service.MarketDataServiceTest` : verification des prix initiaux, mise a jour et simulation quotidienne.
- `service.PortfolioServiceTest` : verification des achats, ventes, recalcul du PRU et levees d'exceptions.
- `service.AnalyticsServiceTest` : calcul des plus-values latentes et estimation de la taxe de vente.
- `EndToEndPortfolioFlowTest` : scenario complet combinant chargement, simulation de marche et transactions.
