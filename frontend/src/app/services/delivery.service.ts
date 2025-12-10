import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class DeliveryService {
  private apiUrl = 'http://localhost:8080/api/deliveries';

  constructor(private http: HttpClient, private authService: AuthService) { }

  getDeliveryStats(): Observable<any> {
    const headers = this.authService.getHeaders();
    return this.http.get<any>(`${this.apiUrl}/stats`, { headers });
  }

  getRecentDeliveries(limit: number = 5): Observable<any[]> {
    const headers = this.authService.getHeaders();
    return this.http.get<any[]>(`${this.apiUrl}/recent?limit=${limit}`, { headers });
  }

  getAllDeliveries(): Observable<any[]> {
    const headers = this.authService.getHeaders();
    return this.http.get<any[]>(`${this.apiUrl}`, { headers });
  }

  getDeliveryById(id: string): Observable<any> {
    const headers = this.authService.getHeaders();
    return this.http.get<any>(`${this.apiUrl}/${id}`, { headers });
  }

  createDelivery(delivery: any): Observable<any> {
    const headers = this.authService.getHeaders();
    return this.http.post<any>(`${this.apiUrl}`, delivery, { headers });
  }

  updateDelivery(id: string, delivery: any): Observable<any> {
    const headers = this.authService.getHeaders();
    return this.http.put<any>(`${this.apiUrl}/${id}`, delivery, { headers });
  }

  deleteDelivery(id: string): Observable<any> {
    const headers = this.authService.getHeaders();
    return this.http.delete<any>(`${this.apiUrl}/${id}`, { headers });
  }

  cancelDelivery(id: string, reason: string): Observable<any> {
    const headers = this.authService.getHeaders();
    return this.http.post<any>(`${this.apiUrl}/${id}/cancel`, { reason }, { headers });
  }
}
