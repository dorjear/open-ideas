// Debugging script for Azure Cognitive Search query

const axios = require('axios');

// Configuration
const AZURE_SEARCH_SERVICE_NAME = "servicelu";
const AZURE_SEARCH_INDEX_NAME = "ai-search-1751681781733";
const AZURE_SEARCH_API_KEY = "your-search-api-key"; // Replace with your actual API key

// Debugging function
async function debugQueryIndex(queryVector) {
  const endpoint = `https://${AZURE_SEARCH_SERVICE_NAME}.search.windows.net/indexes/${AZURE_SEARCH_INDEX_NAME}/docs/search?api-version=2025-05-01-preview`;
  const headers = {
    "Content-Type": "application/json",
    "api-key": AZURE_SEARCH_API_KEY,
  };
  const body = {
    count: true,
    vectorQueries: [
      {
        kind: "text",
        text: "sydnety",
        fields: "text_vector",
      },
    ],
  };

  console.log("Request Body:", JSON.stringify(body, null, 2));
  console.log("Endpoint:", endpoint);

  try {
    const response = await axios.post(endpoint, body, { headers });
    console.log("Query Results:", response.data.value);
  } catch (error) {
    if (error.response) {
      console.error("Error Response Code:", error.response.status);
      console.error("Error Response Data:", error.response.data);
    } else {
      console.error("Error Message:", error.message);
    }
  }
}

// Example usage
const exampleQueryVector = "sydny"; // Replace with your actual query vector
debugQueryIndex(exampleQueryVector);
