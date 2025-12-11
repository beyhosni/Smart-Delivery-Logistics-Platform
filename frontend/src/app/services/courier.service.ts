import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class CourierService {
  private apiUrl = 'http://localhost:8082/api/dispatchers';

  constructor(private http: HttpClient, private authService: AuthService) { }

  /**
   * Récupère un livreur par son ID
   * @param id ID du livreur
   */
  getCourierById(id: string): Observable<any> {
    const headers = this.authService.getHeaders();
    return this.http.get<any>(`${this.apiUrl}/couriers/${id}`, { headers });
  }

  /**
   * Met à jour le statut d'un livreur
   * @param id ID du livreur
   * @param status Nouveau statut
   */
  updateCourierStatus(id: string, status: string): Observable<any> {
    const headers = this.authService.getHeaders();
    return this.http.put<any>(
      `${this.apiUrl}/couriers/${id}/status`,
      { status },
      { headers }
    );
  }

  /**
   * Met à jour la position d'un livreur
   * @param id ID du livreur
   * @param latitude Latitude
   * @param longitude Longitude
   * @param address Adresse
   */
  updateCourierLocation(id: string, latitude: number, longitude: number, address?: string): Observable<any> {
    const headers = this.authService.getHeaders();
    return this.http.put<any>(
      `${this.apiUrl}/couriers/${id}/location`,
      { latitude, longitude, address },
      { headers }
    );
  }

  /**
   * Récupère les livraisons d'un livreur
   * @param id ID du livreur
   * @param status Statut des livraisons (optionnel)
   */
  getDeliveriesByCourierId(id: string, status?: string): Observable<any[]> {
    const headers = this.authService.getHeaders();
    let url = `${this.apiUrl}/couriers/${id}/deliveries`;

    if (status) {
      url += `?status=${status}`;
    }

    return this.http.get<any[]>(url, { headers });
  }

  /**
   * Récupère les statistiques d'un livreur
   * @param id ID du livreur
   * @param period Période (jour, semaine, mois)
   */
  getCourierStats(id: string, period: string = 'day'): Observable<any> {
    const headers = this.authService.getHeaders();
    return this.http.get<any>(
      `${this.apiUrl}/couriers/${id}/stats?period=${period}`,
      { headers }
    );
  }

  /**
   * Récupère les livreurs disponibles
   * @param latitude Latitude centrale
   * @param longitude Longitude centrale
   * @param radius Rayon de recherche en km
   */
  getAvailableCouriers(latitude: number, longitude: number, radius: number = 10): Observable<any[]> {
    const headers = this.authService.getHeaders();
    return this.http.get<any[]>(
      `${this.apiUrl}/couriers/available?lat=${latitude}&lng=${longitude}&radius=${radius}`,
      { headers }
    );
  }

  /**
   * Récupère tous les livreurs
   * @param status Statut des livreurs (optionnel)
   */
  getAllCouriers(status?: string): Observable<any[]> {
    const headers = this.authService.getHeaders();
    let url = `${this.apiUrl}/couriers`;

    if (status) {
      url += `?status=${status}`;
    }

    return this.http.get<any[]>(url, { headers });
  }
}
