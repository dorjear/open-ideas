# Guide to Uploading Files and Embedding Them via Azure Portal

## Step 1: Upload Files to Azure Cognitive Search
1. **Access Azure Portal**:
   - Navigate to your Azure Cognitive Search service in the Azure Portal.

2. **Create or Update an Index**:
   - Define an index schema that includes fields for document content and embeddings.
   - Example fields:
     - `id`: Unique identifier for the document.
     - `content`: Text content of the document.
     - `embedding`: Array of numbers representing the embedding vector.

3. **Upload Documents**:
   - Use the "Import Data" feature in the Azure Portal to upload files.
   - Select a data source (e.g., Azure Blob Storage, SQL Database).
   - Map the fields in your data source to the index schema.

## Step 2: Generate Embeddings
Azure Cognitive Search does not natively generate embeddings. You need to:
1. Use Azure OpenAI Service to generate embeddings for your documents.
2. Update the index with the embeddings using the Azure Cognitive Search REST API.

### Example Workflow
1. **Generate Embeddings**:
   - Use the Azure OpenAI Service API to create embeddings for your document content.

2. **Update Index**:
   - Use the Azure Cognitive Search REST API to add the embeddings to the index.

### Example API Request
```json
{
  "value": [
    {
      "@search.action": "mergeOrUpload",
      "id": "document-id",
      "embedding": [0.1, 0.2, 0.3, ...]
    }
  ]
}
```

## Notes
- Direct upload via the Azure Portal is limited to document content. Embeddings must be generated externally and added via API.
- Ensure your index schema is designed to accommodate embeddings.
- Use Azure Blob Storage for large-scale document uploads.