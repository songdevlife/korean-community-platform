function UpdateCard({ update }) {
    return (
      <div className="bg-white rounded-2xl shadow-sm p-4 border border-gray-100">
        <div className="flex items-center gap-2 mb-2">
          {update.category && (
            <span className="text-xs bg-cream border border-gray-200 px-2 py-0.5 rounded-full text-ink">
              {update.category.name}
            </span>
          )}
          <span className="text-xs text-gray-400">{update.geographicScope}</span>
          {update.aiGenerated && (
            <span className="text-xs text-korea-blue">✨ AI 요약</span>
          )}
        </div>
        <h3 className="font-semibold text-ink">{update.title}</h3>
      </div>
    );
  }
  
  export default UpdateCard;