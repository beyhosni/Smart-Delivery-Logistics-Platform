import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private socket: WebSocketSubject<any>;
  private connection$: Subject<boolean> = new Subject<boolean>();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectInterval = 5000; // 5 secondes

  constructor() {
    this.connection$.next(false);
  }

  /**
   * Établit une connexion WebSocket
   * @param url URL du WebSocket
   */
  public connect(url: string): void {
    if (!this.socket || this.socket.closed) {
      this.socket = webSocket(url);
      this.connection$.next(true);

      // Gérer les reconnexions automatiques
      this.socket.subscribe({
        next: (message) => {
          // Les messages sont gérés par les composants qui s'abonnent
          this.reconnectAttempts = 0; // Réinitialiser le compteur de reconnexion
        },
        error: (error) => {
          console.error('WebSocket error:', error);
          this.handleReconnect(url);
        },
        complete: () => {
          console.log('WebSocket connection closed');
          this.connection$.next(false);
          this.handleReconnect(url);
        }
      });
    }
  }

  /**
   * Ferme la connexion WebSocket
   */
  public disconnect(): void {
    if (this.socket) {
      this.socket.complete();
      this.connection$.next(false);
    }
  }

  /**
   * Vérifie si la connexion est active
   */
  public isConnected(): Observable<boolean> {
    return this.connection$.asObservable();
  }

  /**
   * S'abonne à un topic spécifique
   * @param topic Topic auquel s'abonner
   * @returns Observable des messages du topic
   */
  public subscribeToTopic(topic: string): Observable<any> {
    if (!this.socket) {
      throw new Error('WebSocket connection not established');
    }

    return new Observable(observer => {
      const subscription = this.socket.subscribe({
        next: (message) => {
          // Filtrer les messages par topic
          if (message && message.topic === topic) {
            observer.next(message.data);
          }
        },
        error: (error) => observer.error(error),
        complete: () => observer.complete()
      });

      return {
        unsubscribe: () => subscription.unsubscribe()
      };
    });
  }

  /**
   * Envoie un message via WebSocket
   * @param message Message à envoyer
   */
  public sendMessage(message: any): void {
    if (this.socket && !this.socket.closed) {
      this.socket.next(message);
    } else {
      console.error('WebSocket connection not established');
    }
  }

  /**
   * Gère la reconnexion automatique
   * @param url URL du WebSocket
   */
  private handleReconnect(url: string): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);

      setTimeout(() => {
        this.connect(url);
      }, this.reconnectInterval);
    } else {
      console.error('Max reconnection attempts reached');
    }
  }
}
