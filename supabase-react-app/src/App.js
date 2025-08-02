import React, { useState, useEffect } from 'react';
import { createClient } from '@supabase/supabase-js';

const supabaseUrl = 'https://kdbrlesgpbhzlqqftaqb.supabase.co';
const supabaseKey = 'your supabase key';
const supabase = createClient(supabaseUrl, supabaseKey);

function App() {
  const [rates, setRates] = useState([]);
  const [rate, setRate] = useState('');
  const [rateType, setRateType] = useState('');
  const [updateId, setUpdateId] = useState('');
  const [newUpdateRate, setUpdateRate] = useState('');
  const [updateRateType, setUpdateRateType] = useState('');
  const [deleteId, setDeleteId] = useState('');

  useEffect(() => {
    fetchRates();
  }, []);

  const fetchRates = async () => {
    const { data, error } = await supabase.from('homeloan-rate').select('*');
    if (error) {
      console.error('Error fetching rates:', error);
    } else {
      setRates(data);
    }
  };

  const createRate = async () => {
    const { data, error } = await supabase
      .from('homeloan-rate')
      .insert([{ rate, rateType }]);
    if (error) {
      console.error('Error creating rate:', error);
    } else {
      console.log('Rate created:', data);
      fetchRates();
    }
  };

  const handleUpdateRate = async () => {
    const { data, error } = await supabase
      .from('homeloand-rate')
      .update({ rate: newUpdateRate, rateType: updateRateType })
      .eq('id', updateId);
    if (error) {
      console.error('Error updating rate:', error);
    } else {
      console.log('Rate updated:', data);
      fetchRates();
    }
  };

  const deleteRate = async () => {
    const { data, error } = await supabase
      .from('homeloan-rate')
      .delete()
      .eq('id', deleteId);
    if (error) {
      console.error('Error deleting rate:', error);
    } else {
      console.log('Rate deleted:', data);
      fetchRates();
    }
  };

  return (
    <div className="container">
      <h1>Supabase CRUD Operations</h1>
      <div>
        <h2>Create Rate</h2>
        <input
          type="text"
          placeholder="Rate"
          value={rate}
          onChange={(e) => setRate(e.target.value)}
        />
        <input
          type="text"
          placeholder="Rate Type"
          value={rateType}
          onChange={(e) => setRateType(e.target.value)}
        />
        <button onClick={createRate}>Create</button>
      </div>
      <div className="container">
        <h2>Read Rates</h2>
        <button onClick={fetchRates}>Refresh</button>
        <ul>
          {rates.map((r) => (
            <li key={r.id}>
              ID: {r.id}, Rate: {r.rate}, Rate Type: {r.rateType}, Created At:{' '}
              {r.created_at}
            </li>
          ))}
        </ul>
      </div>
      <div>
        <h2>Update Rate</h2>
        <input
          type="text"
          placeholder="ID"
          value={updateId}
          onChange={(e) => setUpdateId(e.target.value)}
        />
        <input
          type="text"
          placeholder="New Rate"
          value={newUpdateRate}
          onChange={(e) => setUpdateRate(e.target.value)}
        />
        <input
          type="text"
          placeholder="New Rate Type"
          value={updateRateType}
          onChange={(e) => setUpdateRateType(e.target.value)}
        />
        <button onClick={handleUpdateRate}>Update</button>
      </div>
      <div>
        <h2>Delete Rate</h2>
        <input
          type="text"
          placeholder="ID"
          value={deleteId}
          onChange={(e) => setDeleteId(e.target.value)}
        />
        <button onClick={deleteRate}>Delete</button>
      </div>
    </div>
  );
}

export default App;
