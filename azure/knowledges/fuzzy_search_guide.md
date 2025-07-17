# Guide to Enabling Fuzzy Search in Azure Cognitive Search

## Problem
In simple search mode, exact keyword matches are required to retrieve results. This limits the ability to find relevant documents when the query is slightly different from the indexed content.

## Solution: Enable Fuzzy Search
Azure Cognitive Search supports fuzzy search using the `search` parameter with the `~` operator.

### Example Query
To enable fuzzy search, append `~` followed by a number to the keyword. The number specifies the edit distance (e.g., `1` allows one character difference).

#### Query Example
```json
{
  "search": "keyword~1"
}
```

### Steps to Implement Fuzzy Search
1. Modify the query payload in your script to include the `~` operator for fuzzy search.
2. Test the query with different edit distances to find the optimal value for your use case.

#### Updated Query Payload
```json
{
  "search": "example~2"
}
```

## Notes
- Fuzzy search increases the flexibility of keyword matching but may reduce precision.
- Experiment with different edit distances to balance recall and precision.