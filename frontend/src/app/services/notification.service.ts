import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private apiUrl = 'http://localhost:8084/api/notifications';
  private notificationSubject = new Subject<any>();
  public notifications$ = this.notificationSubject.asObservable();

  constructor(private http: HttpClient, private authService: AuthService) { }

  // Méthode pour émettre des notifications côté client
  showNotification(notification: any): void {
    this.notificationSubject.next(notification);
  }

  // Récupérer les notifications depuis le serveur
  getUserNotifications(): Observable<any[]> {
    const headers = this.authService.getHeaders();
    return this.http.get<any[]>(`${this.apiUrl}/user`, { headers });
  }

  // Marquer une notification comme lue
  markAsRead(notificationId: string): Observable<any> {
    const headers = this.authService.getHeaders();
    return this.http.put<any>(`${this.apiUrl}/${notificationId}/read`, {}, { headers });
  }

  // Marquer toutes les notifications comme lues
  markAllAsRead(): Observable<any> {
    const headers = this.authService.getHeaders();
    return this.http.put<any>(`${this.apiUrl}/read-all`, {}, { headers });
  }

  // S'abonner aux notifications en temps réel (WebSocket)
  subscribeToRealTimeNotifications(): Observable<any> {
    // Dans un vrai projet, ceci utiliserait WebSocket pour des notifications en temps réel
    // Pour cet exemple, nous simulons avec des intervalles
    return new Observable(observer => {
      const interval = setInterval(() => {
        this.getUserNotifications().subscribe({
          next: (notifications) => {
            // Filtrer les notifications non lues
            const unreadNotifications = notifications.filter(n => !n.read);
            if (unreadNotifications.length > 0) {
              observer.next({
                type: 'NEW_NOTIFICATIONS',
                count: unreadNotifications.length,
                notifications: unreadNotifications
              });
            }
          },
          error: (err) => {
            observer.error(err);
          }
        });
      }, 30000); // Vérification toutes les 30 secondes

      // Nettoyage lors de la désinscription
      return {
        unsubscribe() {
          clearInterval(interval);
        }
      };
    });
  }
}
