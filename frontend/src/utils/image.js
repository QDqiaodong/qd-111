import Compressor from 'compressorjs'

export function compressImage(file, options = {}) {
  return new Promise((resolve, reject) => {
    new Compressor(file, {
      quality: options.quality || 0.7,
      maxWidth: options.maxWidth || 1280,
      maxHeight: options.maxHeight || 1280,
      mimeType: 'image/jpeg',
      convertSize: 1024 * 1024,
      success: (compressedFile) => {
        resolve(new File([compressedFile], file.name.replace(/\.[^.]+$/, '.jpg'), { type: 'image/jpeg' }))
      },
      error: (err) => {
        reject(err)
      }
    })
  })
}

export async function compressImageList(files, options) {
  const results = []
  for (const file of files) {
    try {
      const compressed = await compressImage(file, options)
      results.push(compressed)
    } catch {
      results.push(file)
    }
  }
  return results
}

export function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}
