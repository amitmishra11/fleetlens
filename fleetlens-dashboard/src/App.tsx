import { Route, Routes } from 'react-router-dom';
import { Layout } from './components/shared/Layout';
import { ConfigAudit } from './pages/ConfigAudit';
import { Dashboard } from './pages/Dashboard';
import { MemoryProfiler } from './pages/MemoryProfiler';
import { SchemaDrift } from './pages/SchemaDrift';
import { ServiceDetail } from './pages/ServiceDetail';
import { TraceReplay } from './pages/TraceReplay';

function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/services/:id" element={<ServiceDetail />} />
        <Route path="/schema" element={<SchemaDrift />} />
        <Route path="/traces" element={<TraceReplay />} />
        <Route path="/memory" element={<MemoryProfiler />} />
        <Route path="/config" element={<ConfigAudit />} />
      </Route>
    </Routes>
  );
}

export default App;
