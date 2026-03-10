import './App.css'

function App() {
  return (
    <div className="app-root">
      <header className="app-header">
        <h1>City Event Monitor</h1>
        <span className="app-subtitle">Kocaeli kentsel olay haritası</span>
      </header>

      <main className="app-main">
        <section className="filters-panel">
          <h2>Filtreler</h2>
          <p>Buraya haber türü, ilçe ve tarih filtreleri gelecek.</p>
        </section>

        <section className="content-panel">
          <div className="map-container">
            <h2>Harita</h2>
            <p>Google Maps bileşeni burada yer alacak.</p>
          </div>
          <div className="list-container">
            <h2>Haber Listesi</h2>
            <p>Seçili filtrelere göre haberler burada listelenecek.</p>
          </div>
        </section>
      </main>
    </div>
  )
}

export default App
