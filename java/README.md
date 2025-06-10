# Proyecto: Azure Durable Functions con Java y Python

Esta seccion en JAVA simula la invocacion de un modelo analitico en python en uno de los activites del durable functions. Dicha simulacion consiste de 3 pasos:

1. **Armado de datos**: Prepara los datos necesarios para el flujo.
2. **Invocación de un API en Python**: Llama a un endpoint desarrollado en Python.
3. **Simulación**: Realiza una simulación o procesamiento adicional.

## Estructura del Proyecto

- `/java/`: Contiene la Durable Function y las activities en Java.
- `/python/`: Contiene el API en Python que será invocado desde la función.

## Pasos para Implementar

### 1. Crear el proyecto de Durable Functions en Java

```bash
mvn archetype:generate \
    -DarchetypeGroupId=com.microsoft.azure \
    -DarchetypeArtifactId=azure-functions-archetype
```

### 2. Implementar las Activities

La clase DurableFunctionHandler repartira los activites en metodos:

- **Armado de datos**: Crear una activity que prepare los datos.
- **Invocación de API Python**: Crear una activity que realice una llamada HTTP al endpoint Python.
- **Simulación**: Crear una activity para la simulación.

### 3. Desplegar la Durable Function en Azure

```bash
# Iniciar sesión en Azure
az login

# Crear un grupo de recursos
az group create --name <nombre-grupo> --location <ubicacion>

# Crear una cuenta de almacenamiento (si no tienes una)
az storage account create --name <nombre-storage> --location <ubicacion> --resource-group <nombre-grupo> --sku Standard_LRS

# Crear un plan de App Service
az functionapp plan create --name <nombre-plan> --resource-group <nombre-grupo> --location <ubicacion> --number-of-workers 1 --sku B1 --is-linux

# Crear la Function App
az functionapp create --resource-group <nombre-grupo> --os-type Linux --consumption-plan-location <ubicacion> --runtime java --runtime-version 11 --functions-version 4 --name <nombre-app> --storage-account <nombre-storage>

# Probar en local
mvn clean package
func start --verbose 

# Desplegar la funcion

func azure functionapp publish <nombre-app>

# Probamos la ejecucion
https://<nombre-app>.azurewebsites.net/api/<nombre-de-tu-función-http>

```

### 4. Publicar el API en Python

```bash
# Crear el entorno virtual
python -m venv venv
source venv/bin/activate

# Instalar dependencias
pip install -r requirements.txt

# Para correrlo en local
func init . --python
func start

# Probar en local
curl -X POST http://localhost:7071/api/process \
     -H "Content-Type: application/json" \
     -d '{"input": "Hola desde Java"}'

# Creamos la funcion en Azure (ejemplo : modelo-analitico-fn)
az functionapp create \
  --resource-group <nombre-grupo> \
  --os-type Linux \
  --consumption-plan-location <ubicacion> \
  --runtime python \
  --runtime-version 3.11 \
  --functions-version 4 \
  --name modelo-analitico-fn \
  --storage-account <nombre-storage>

# Publicamos la funcion
func azure functionapp publish modelo-analitico-fn
```
Reemplaza la ruta en el InvokePythonActivity. 


### 5. Configurar la comunicación

Asegúrate de que la Durable Function tenga la URL del endpoint Python y los permisos necesarios para invocarlo.

---

## Recursos útiles

- [Documentación Azure Durable Functions](https://docs.microsoft.com/azure/azure-functions/durable/durable-functions-overview?tabs=java)
- [Documentación Azure Functions para Python](https://docs.microsoft.com/azure/azure-functions/functions-reference-python)
