# CoPurchase Analysis - Google Dataproc

Questo progetto esegue un'analisi sugli acquisti congiunti utilizzando Google Cloud Dataproc.

## Requisiti

1. Google Cloud SDK installato

2. SBT (Scala Build Tool) installato

2. Apache Spark

## Configurazione Google Cloud

1. Accedi a Google Cloud Console e crea un nuovo progetto

2. Autenticarsi con Google Cloud SDK
   ```sh
   gcloud auth login
   ```

3. Configurare il progetto
   ```sh
   gcloud config set project <id_progetto>
   ```

## Creazione del Bucket di Storage

Creare un bucket su Google Cloud Storage per ospitare il file JAR e il dataset:

```sh
gcloud storage buckets create gs://<bucket_name> --location=europe-west1
```

## Preparazione del file JAR

1. **Generare il file JAR**
   Nel progetto in Visual Studio Code, eseguire il seguente comando:
   ```sh
   sbt package
   ```
   Il file JAR verrà generato nella directory `target/scala-2.12`.

2. **Caricare il file JAR sul bucket**
   ```sh
   gsutil cp <percorso_del_file_jar> gs://<bucket_name>/
   ```

## Caricamento del Dataset

Caricare il dataset su Google Cloud Storage:

```sh
gsutil cp <percorso_del_dataset> gs://<bucket_name>/
```

## Creazione del Cluster Dataproc

Creare un cluster Dataproc con 2 nodi e un disco di 240 GB:

```sh
gcloud dataproc clusters create copurchase-cluster \
    --region=europe-west1 \
    --enable-component-gateway \
    --num-workers=2 \
    --master-boot-disk-size=240 \
    --worker-boot-disk-size=240
```

> **Nota:** Se si verifica un errore nell'abilitazione delle API, accedere alla dashboard di Google Cloud Console, navigare in **Reti VPC**, selezionare **Subnet**, cercare la rete "default" della regione `europe-west1` e abilitare l'accesso privato Google.

## Esecuzione del Job su Dataproc

Eseguire il job sul cluster con il seguente comando:

```sh
gcloud dataproc jobs submit spark \
    --cluster=copurchase-cluster \
    --region=europe-west1 \
    --jar=gs://<bucket_name>/<file_jar> \
    -- input_path=gs://<bucket_name>/<dataset>
    output_path=gs://<bucket_name>/output/
```

## Conclusione

Dopo l'esecuzione del job, i risultati saranno disponibili nella cartella `output/` all'interno del bucket specificato.

---

**Autore:** Andrea Napoli

