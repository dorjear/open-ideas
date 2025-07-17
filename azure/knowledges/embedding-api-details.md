# API Details for Embedding and Retrieval

## Azure OpenAI Service API
### Endpoint
`https://<region>.api.cognitive.microsoft.com/openai/deployments/<deployment-id>/embeddings`

### Method
POST

### Headers
- `Content-Type`: application/json
- `Authorization`: Bearer <API-Key>

### Request Body
```json
{
  "input": "Your document content here"
}
```

### Response
```json
{
  "data": [
    {
      "embedding": [0.1, 0.2, 0.3, ...]
    }
  ]
}
```

---

## Azure Cognitive Search REST API
### Indexing Documents
#### Endpoint
`https://<search-service-name>.search.windows.net/indexes/<index-name>/docs/index`

#### Method
POST

#### Headers
- `Content-Type`: application/json
- `api-key`: <API-Key>

#### Request Body
```json
{
  "value": [
    {
      "@search.action": "upload",
      "id": "1",
      "content": "Your document content here",
      "embedding": [0.1, 0.2, 0.3, ...]
    }
  ]
}
```

#### Response
```json
{
  "status": "success"
}
```

---

### Querying Documents
#### Endpoint
`https://<search-service-name>.search.windows.net/indexes/<index-name>/docs/search`

#### Method
POST

#### Headers
- `Content-Type`: application/json
- `api-key`: <API-Key>

#### Request Body
```json
{
  "search": "Your query vector here",
  "queryType": "vector",
  "fields": ["embedding"]
}
```

#### Response
```json
{
  "value": [
    {
      "id": "1",
      "content": "Your document content here",
      "score": 0.95
    }
  ]
}
```

---

## Notes
- Replace `<region>`, `<deployment-id>`, `<search-service-name>`, and `<index-name>` with your actual Azure service details.
- Ensure proper API key management and secure storage.