import logging
import time
import azure.functions as func
import json

app = func.FunctionApp()

@app.route(route="process", auth_level=func.AuthLevel.ANONYMOUS)
def main(req: func.HttpRequest) -> func.HttpResponse:
    logging.info('Python function received a request.')

    try:
        data = req.get_json()
        input_value = data.get('input', 'No input provided')
    except ValueError:
        return func.HttpResponse("Invalid JSON", status_code=400)

    logging.info(f"Simulating 1-minute task for input: {input_value}")
    time.sleep(60)  # Simula trabajo pesado (1 minuto)

    result = {
        "status": "done",
        "original_input": input_value,
        "processed": input_value[::-1]  # Solo invierte el string como ejemplo
    }

    return func.HttpResponse(json.dumps(result), mimetype="application/json")