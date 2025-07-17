# Guide to Performing Vector Search in Azure Cognitive Search

## Overview
Vector search allows you to retrieve documents based on semantic similarity using embedding vectors. Azure Cognitive Search supports vector search in preview mode.

## Prerequisites
1. Ensure your index schema includes a field for embeddings (e.g., `embedding`).
2. Use Azure OpenAI Service to generate embeddings for your documents and store them in the index.

## Steps to Perform Vector Search

### Step 1: Update Index Schema
Ensure your index schema includes the following:
- `embedding`: A collection of numbers representing the embedding vector.

### Step 2: Query Payload
Use the `searchFields` parameter to specify the embedding field and provide the query vector.

#### Example Query Payload
```json
{
  "searchFields": "embedding",
  "vector": [0.1, 0.2, 0.3, 0.4, 0.5],
  "top": 5
}
```

### Step 3: API Request
Send the query payload to the Azure Cognitive Search REST API.

#### Example Request
```json
POST https://<search-service-name>.search.windows.net/indexes/<index-name>/docs/search?api-version=2021-04-30-Preview
Content-Type: application/json
api-key: <your-api-key>

{
  "searchFields": "embedding",
  "vector": [0.1, 0.2, 0.3, 0.4, 0.5],
  "top": 5
}
```

### Step 4: Parse Results
The response will include documents ranked by semantic similarity to the query vector.

## Notes
- Vector search is available in preview mode and requires the correct API version (`2021-04-30-Preview`).
- Ensure embeddings are normalized for better results.